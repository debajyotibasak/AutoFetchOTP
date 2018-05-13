package com.debajyotibasak.autofetchotp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class IncomingSmsReceiver extends BroadcastReceiver {

    private SmsListener smsListener;

    @Override
    public void onReceive(Context context, Intent intent) {

        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                for (Object aPdusObj : pdusObj) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj);
                    String senderNum = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();
                    try {
                        if (senderNum.contains("WAYSMS")) {
                            smsListener.messageReceived(getVerificationCode(message));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getVerificationCode(String message) {
        List<String> msg = Arrays.asList(message.split(":"));
        String code = msg.get(2).trim();
        code = code.substring(0, 4);
        Log.d("smss", code + "");
        return code;
    }

    public void bindListener(SmsListener listener){
        smsListener = listener;
    }

    public interface SmsListener {
        void messageReceived(String messageText);
    }
}
