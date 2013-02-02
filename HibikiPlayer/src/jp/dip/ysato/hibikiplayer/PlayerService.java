package jp.dip.ysato.hibikiplayer;


import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.VitamioInstaller.VitamioNotCompatibleException;
import io.vov.vitamio.VitamioInstaller.VitamioNotFoundException;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class PlayerService extends Service {

	public static final String START = "START";
	public static final String PAUSE = "PAUSE";
	public static final String PLAY = "PLAY";
	public static final String SEEK = "SEEK";
	public static final String RESUME = "RESUME";
	private MediaPlayer mediaPlayer;
	private ScheduledFuture<?> monitorThread;
	private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
	private PlayerActivity activity;
	private boolean manualPause;
	private NotificationManager notificationManager;
	private String url;
	private Bitmap bitmap;
	private String title;
	private int no;
	private WakeLock wakeLock;

	class PlayMonitor implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				if (activity != null) {
					if (mediaPlayer.isPlaying()) {
						int position = (int) (mediaPlayer.getCurrentPosition() / 1000);
						activity.setPosition(position);
					} else if (!manualPause)
						mediaPlayer.prepareAsync();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setNotification(String title) {
		// TODO Auto-generated method stub
		Notification n = new Notification(R.drawable.ic_launcher, getString(R.string.playNotification, title), 
				System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT;
		Intent intent = new Intent(this, PlayerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("url", url);
		bundle.putParcelable("image", bitmap);
		bundle.putString("program", title);
		bundle.putInt("no", no);
		intent.putExtra("program", bundle);
		intent.setAction(RESUME);
		PendingIntent ci = PendingIntent.getActivity(this, 0, intent, 0);
		n.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.playNotification, title), ci);
		notificationManager.notify(R.string.app_name, n);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return new PlayerServiceBinder();
	}
	class PlayerServiceBinder extends Binder {
		public void registerActivity(PlayerActivity activity) {
			PlayerService.this.activity = activity;
		}
	}
	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mediaPlayer = null;
		PowerManager powerManager = (PowerManager) getSystemService(getBaseContext().POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		wakeLock.release();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent == null)
			return START_STICKY;
		String action = intent.getAction();
		if (action.equals(START)) {
			Bundle	bundle = intent.getBundleExtra("program");
			url = bundle.getString("url");
			bitmap = bundle.getParcelable("image");
			title = bundle.getString("program");
			no = bundle.getInt("no");
			try {
				if (mediaPlayer != null) {
						mediaPlayer.stop();
						mediaPlayer.release();
						mediaPlayer = null;
						notificationManager.cancel(R.string.app_name);
				}
				mediaPlayer = new MediaPlayer(this);
				mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer arg0) {
						if (activity != null) {
							int duration = (int)arg0.getDuration() / 1000;
							activity.setDuration(duration);	
						}
						setNotification(title);
						if (monitorThread != null)
							monitorThread.cancel(true);
						monitorThread = threadPool.scheduleAtFixedRate(new PlayMonitor(), 0, 500, TimeUnit.MILLISECONDS);
						arg0.start();
					}
				});
				mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer arg0) {
						// TODO Auto-generated method stub
						arg0.release();
					}
				});
				mediaPlayer.setDataSource(url);
				mediaPlayer.setDisplay(null);
				mediaPlayer.prepareAsync();
				manualPause = false;
				wakeLock.acquire();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (VitamioNotCompatibleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (VitamioNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (action.equals(RESUME) && mediaPlayer != null) {
			if (activity != null) {
				int duration = (int)mediaPlayer.getDuration() / 1000;
				activity.setDuration(duration);	
			}
		}
		if (action.equals(PAUSE) && !manualPause) {
			manualPause = true;
			mediaPlayer.pause();
			wakeLock.release();
			notificationManager.cancel(R.string.app_name);
			if (activity != null)
				activity.setPlayState(false);
		}
		if (action.equals(PLAY) && manualPause) {
			manualPause = false;
			mediaPlayer.start();
			wakeLock.acquire();
			setNotification(title);
			if (activity != null)
				activity.setPlayState(true);
		}
		
		return START_STICKY;
	}
}
