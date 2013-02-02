package jp.dip.ysato.hibikiplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class EventReceiver extends BroadcastReceiver {

	public static final String PAUSE = EventReceiver.class.getPackage().toString() + ".PAUSE";
	public static final String PLAYPAUSE = EventReceiver.class.getPackage().toString() + ".PLAYPAUSE";
	private long lasttime;
	private Object lastaction;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if ((System.currentTimeMillis() - lasttime) < 1000 && action.equals(lastaction))
			return;
		lasttime = System.currentTimeMillis();
		lastaction = action;
		Intent broadcastIntent = new Intent();
		if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			broadcastIntent.setAction(PAUSE);
		}
		if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
			broadcastIntent.setAction(PLAYPAUSE);
		}
		context.sendBroadcast(broadcastIntent);
	}

}
