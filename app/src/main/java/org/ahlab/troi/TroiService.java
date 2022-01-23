package org.ahlab.troi;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

public class TroiService extends Service {
	private EmpaticaListener empaticaListener;
	private int ONGOING_NOT_ID = 99;

	public TroiService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		empaticaListener = EmpaticaListener.getInstance();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new Notification.Builder(this, "CHANNEL_DEFAULT_IMPORTANCE").setContentTitle("Troi Service").setContentText("Troi is running in background").setContentIntent(pendingIntent).build();
		startForeground(ONGOING_NOT_ID, notification);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}