package jp.dip.ysato.hibikiplayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlayerActivity extends Activity {
	private Bundle bundle;
	private TextView position;
	public ServiceConnection serviceConnection;
	private Handler handler;
	public boolean playing;
	private BroadcastReceiver receiver;
	private ImageView playControlButton;
	private SeekBar seekbar;
	private String title;
	private int no;
	private Bitmap bitmap;
	private String url;
	private String comment;
	private ScheduledFuture<?> blinkThread;
	private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
	private int playback;
	
	class BlinkTask implements Runnable {
		private boolean state;
		private Handler handle;
		private int pos;
		BlinkTask(Handler handle, int position) {
			state = false;
			this.handle = handle;
			this.pos = position;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			handle.post(new Runnable() {
				@Override
				public void run() {
					position.setText(state?String.format("%02d:%02d", pos / 60, pos % 60):"     ");
				}
			});
			state = !state;
		}
	}
	private void playState(boolean state) {
		// TODO Auto-generated method stub
		if (!state) {
			playControlButton.setImageResource(android.R.drawable.ic_media_play);
			playing = false;
			blinkThread = threadPool.scheduleAtFixedRate(new BlinkTask(handler,playback), 0, 1000, TimeUnit.MILLISECONDS);
		} else {
			playControlButton.setImageResource(android.R.drawable.ic_media_pause);
			if(blinkThread != null) {
				blinkThread.cancel(false);
				blinkThread = null;
			}
			playing = true;
		}
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		Intent intent = getIntent();
		String action = intent.getAction();
		bundle = intent.getBundleExtra("program");
		bitmap = bundle.getParcelable("image");
		url = bundle.getString("url");
		title = bundle.getString("program");
		no = bundle.getInt("no");
		comment = bundle.getString("comment");
		ImageView imageView = (ImageView) findViewById(R.id.playerImage);
		imageView.setImageBitmap(bitmap);
		position = (TextView) findViewById(R.id.position);
		TextView description = (TextView) findViewById(R.id.description);
		if (no > 0)
			description.setText(String.format(getString(R.string.description), title, no));
		else
			description.setText(String.format(getString(R.string.descriptionstr), title, comment));
		handler = new Handler();
    	seekbar = (SeekBar) findViewById(R.id.playPosition);
    	seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(PlayerActivity.this, PlayerService.class);
				intent.setAction(PlayerService.SEEK);
				intent.putExtra("position", arg0.getProgress());
				startService(intent);
			}
    	});
		serviceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName arg0, IBinder arg1) {
				((PlayerService.PlayerServiceBinder)arg1).registerActivity(PlayerActivity.this);
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				// TODO Auto-generated method stub
				
			}
			
		};
		playControlButton = (ImageButton) findViewById(R.id.playControlButton);
		playControlButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(PlayerActivity.this, PlayerService.class);
				intent.setAction(playing?PlayerService.PAUSE:PlayerService.PLAY);
				startService(intent);
			}

		});
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				// TODO Auto-generated method stub
				String action = arg1.getAction();
				ImageButton imageButton = (ImageButton) findViewById(R.id.playControlButton);
				if (action.equals(EventReceiver.PAUSE)) {
					imageButton.setImageResource(android.R.drawable.ic_media_play);
					playing = false;
				}
				if (action.equals(EventReceiver.PLAYPAUSE)) {
					if (!playing) {
						imageButton.setImageResource(android.R.drawable.ic_media_pause);
					} else {
						imageButton.setImageResource(android.R.drawable.ic_media_play);
					}
				}
			}
		};
		if (action.equals(ProgramAdapter.START)) {
			playing = true;
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(EventReceiver.PAUSE);
			intentFilter.addAction(EventReceiver.PLAYPAUSE);
			registerReceiver(receiver, intentFilter);
			intent = new Intent(getBaseContext(), PlayerService.class);
			intent.setAction(PlayerService.START);
			bundle = new Bundle();
			bundle.putString("url", url);
			bundle.putParcelable("image", bitmap);
			bundle.putString("program", title);
			bundle.putInt("no", no);
			intent.putExtra("program", bundle);
			bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
			startService(intent);
		}
		if (action.equals(ProgramAdapter.START)) {
			intent.setAction(PlayerService.RESUME);
			bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
			startService(intent);
		}
	}
	@Override
	public void onDestroy() {
		unbindService(serviceConnection);
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	public void setDuration(int duration) {
		// TODO Auto-generated method stub
		final int d = duration;
		handler.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TextView durationText = (TextView) findViewById(R.id.duration);
				durationText.setText(String.format("%02d:%02d", d / 60, d % 60));
				seekbar.setMax(d);
			}
		});
	}

	public void setPosition(int pos) {
		// TODO Auto-generated method stub
		playback = pos;
		handler.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				position.setText(String.format("%02d:%02d", playback / 60, playback % 60));
				seekbar.setProgress(playback);
			}
		});
	}
	public void setPlayState(boolean b) {
		// TODO Auto-generated method stub
		ImageButton imageButton = (ImageButton) findViewById(R.id.playControlButton);
		if (b == true)
			imageButton.setImageResource(android.R.drawable.ic_media_pause);
		else
			imageButton.setImageResource(android.R.drawable.ic_media_play);
		playing = !b;
		playState(!playing);
	}
}
