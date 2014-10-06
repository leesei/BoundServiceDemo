package com.leesei.boundservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class BoundService extends Service {
    private final String TAG = "BoundService";

    private final IBinder localBinder = new LocalBinder();
    private MessageHandler mMessageHandler;
    private Messenger mMessenger;

    // MESSAGE IDs
    private static final int MSG_CLIENT_CONNECTS = 5000;
    private static final int MSG_CLIENT_BIND = 5001;
    private static final int MSG_CLIENT_UNBIND = 5002;
    private static final int MSG_SAY_HELLO = 1;
    private static final int MSG_ACK_HELLO = 1001;
    private static final int MSG_SEND_BUNDLE = 2;
    private static final int MSG_ACK_BUNDLE = 1002;
    // MESSAGE IDs

    // Handler that receives messages from the thread
    private final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, String.format("what[%d] 1[%d] 2[%d]", msg.what, msg.arg1, msg.arg2));
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    if (msg.replyTo != null) {
                        try {
                            Message reply = Message.obtain(null, MSG_ACK_HELLO, 0, 0);
                            msg.replyTo.send(reply);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_SEND_BUNDLE:
                    Bundle bundle = msg.getData();
                    Log.i(TAG, String.format("  message[%s]", bundle.getString("message")));

                    if (msg.replyTo != null) {
                        try {
                            Message reply = Message.obtain(null, MSG_ACK_BUNDLE, 0, 0);
                            Bundle replyBundle = new Bundle();
                            replyBundle.putString("message", "message from server");
                            reply.setData(replyBundle);
                            msg.replyTo.send(reply);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private final class ScheduledTask implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "ScheduledTask triggered");
            mMessageHandler.postDelayed(mScheduledTask, 1000);
        }
    }

    private ScheduledTask mScheduledTask = new ScheduledTask();

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
//        HandlerThread thread = new HandlerThread("ServiceThread",
//                Process.THREAD_PRIORITY_BACKGROUND);
//        thread.start();
//        mMessageHandler = new MessageHandler(thread.getLooper());

        mMessageHandler = new MessageHandler(getMainLooper());

        mMessenger = new Messenger(mMessageHandler);

        mMessageHandler.postDelayed(mScheduledTask, 1000);
        mMessageHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "stopSelf()");
                stopSelf();
            }
        }, 60000);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mMessageHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mMessageHandler.obtainMessage(MSG_CLIENT_CONNECTS, startId, 0);
        mMessageHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()" + intent);
        Message msg = mMessageHandler.obtainMessage(MSG_CLIENT_BIND, 0, 0);
        mMessageHandler.sendMessage(msg);
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()" + intent);
        Message msg = mMessageHandler.obtainMessage(MSG_CLIENT_UNBIND, 0, 0);
        mMessageHandler.sendMessage(msg);
        return false;
    }

    public class LocalBinder extends Binder {
        BoundService getService() {
            return BoundService.this;
        }
    }
}
