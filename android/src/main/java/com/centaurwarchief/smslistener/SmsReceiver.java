package com.centaurwarchief.smslistener;

import android.support.v4.content.WakefulBroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.os.PowerManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class SmsReceiver extends WakefulBroadcastReceiver {
    private ReactApplicationContext mContext;

    private static final String EVENT = "com.centaurwarchief.smslistener:smsReceived";

    public SmsReceiver() {
        super();
    }

    public SmsReceiver(ReactApplicationContext context) {
        mContext = context;
    }

    private void receiveMessage(SmsMessage message) {
        if (mContext == null) {
            return;
        }

        if (! mContext.hasActiveCatalystInstance()) {
            return;
        }

        Log.d(
            SmsListenerPackage.TAG,
            String.format("%s: %s", message.getOriginatingAddress(), message.getMessageBody())
        );

        WritableNativeMap receivedMessage = new WritableNativeMap();

        receivedMessage.putString("originatingAddress", message.getOriginatingAddress());
        receivedMessage.putString("body", message.getMessageBody());

        mContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(EVENT, receivedMessage);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager.WakeLock screenWakeLock;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        screenWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenLock tag from AlarmListener");
        screenWakeLock.acquire();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                receiveMessage(message);
            }

            // if (screenWakeLock != null) {
                screenWakeLock.release();
            // }
            return;
        }

        try {
            final Bundle bundle = intent.getExtras();

            if (bundle == null || ! bundle.containsKey("pdus")) {
                return;
            }

            final Object[] pdus = (Object[]) bundle.get("pdus");

            for (Object pdu : pdus) {
                receiveMessage(SmsMessage.createFromPdu((byte[]) pdu));
            }

            // if (screenWakeLock != null) {
                screenWakeLock.release();
            // }
        } catch (Exception e) {
            Log.e(SmsListenerPackage.TAG, e.getMessage());
            // if (screenWakeLock != null) {
                screenWakeLock.release();
            // }
        }
    }
}
