package com.springcard.android_usb_pcsc.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by matthieu.b on 15/06/2015.
 */
public class SpringCardPCSC {
    /* watermark for logging */
    private static final String TAG = "SpringCardPCSC Library";

    /* package requirement */
    public static final String packageName = "com.springcard.android_usb_pcsc.service";

    /* command list */
    public static final int CMD_READER_STATE = 1;
    public static final int CMD_CARD_STATE = 2;
    public static final int CMD_SEND_APDU = 3;
    public static final int CMD_RECV_RAPDU = 4;
    public static final int CMD_READER_FREE = 5;

    /* error list */
    public static final int ERROR_NO_ERROR = 10;
    public static final int ERROR_NO_READER = 11;
    public static final int ERROR_WRONG_APDU_LENGTH = 12;
    public static final int ERROR_INVALID_APDU = 13;
    public static final int ERROR_CARD_COMMUNICATION = 14;

    /* input messenger used to catch answer from the running service */
    static Messenger saupService = null;

    /* service connection flag */
    public static boolean isBound = false;

    /* calling activity context */
    static Context localContext = null;

    /* output messenger used to talk with the running service */
    static private Messenger localMessenger = null;

    /* callback entry point */
    static Client callback;

    static CardTerminal remoteCardTerminal = null;

    /* callback declaration for async states */
    public interface Client {

        void onTerminalInstalled(CardTerminal terminal);
        void onTerminalRemoved(CardTerminal terminal);
        void onCardInserted(CardTerminal terminal);
        void onCardRemoved(CardTerminal terminal);
        void onResponseAPDU(CardChannel channel,ResponseAPDU response);
    }

    /**
     * Check if service is available on this device
     *
     * @param context
     */
    public static boolean isServiceAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    /**
     * Send an APDU to the card
     * @param command
     */
    public static void sendCommandAPDU(CommandAPDU command) {

        /* Create a new command to the reader */
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();

            /* specify message type */
        bundle.putInt("command", SpringCardPCSC.CMD_SEND_APDU);

            /* apdu contains an apdu to execute on the card */
        bundle.putString("apdu",ByteArrayTohexString(command.getBytes()));

            /* using slot 0 (others slots not used currently) */
        bundle.putInt("slot", 0);

            /* append data to message */
        msg.setData(bundle);

        /* specify caller (for callback) */
        msg.replyTo = localMessenger;
        try {
            saupService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Request access connection/disconnection callback
     */
    private static ServiceConnection saupConnection = new ServiceConnection() {

        /* service connected */
        public void onServiceConnected(ComponentName className, IBinder service) {
            saupService = new Messenger(service);
            isBound = true;

            /******************************/
            /* ask for reader information */
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putInt("command", SpringCardPCSC.CMD_READER_STATE);

            msg.setData(bundle);
            msg.replyTo = localMessenger;
            try {
                saupService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            /***********************/
            /* ask for card status */
            msg = Message.obtain();
            bundle = new Bundle();
            bundle.putInt("command", SpringCardPCSC.CMD_CARD_STATE);

            /* using slot 0 (others slots not used currently) */
            bundle.putInt("slot", 0);

            msg.setData(bundle);
            msg.replyTo = localMessenger;
            try {
                saupService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        /* service disconnected */
        public void onServiceDisconnected(ComponentName className) {
            saupService = null;
            isBound = false;
        }
    };


    /**
     * Try to bind to the service
     *
     * @param context
     */
    public static void bindToService(Context context) {

        /* store context */
        localContext = context;

        /* we need this for callback */
        callback = (Client) context;

        /* create Message receiver */
        localMessenger = new Messenger(new IncomingHandler());

        if(remoteCardTerminal == null) {
            remoteCardTerminal = new CardTerminal(localContext);
        }

         /* bind to SpringCard Android USB PCSC service */
        Intent intent = new Intent("com.springcard.android_usb_pcsc.service");
        intent.setPackage("com.springcard.android_usb_pcsc.service");
        localContext.bindService(intent, saupConnection, Context.BIND_AUTO_CREATE);
    }


    /**
     * Try to unbind from the service
     *
     */
    public static void unBindToService() {

         /* We must clean connexion with SpringCard Android USB PCSC service */
        if (isBound) {

            /******************************/
            /* free this reader           */
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putInt("command", SpringCardPCSC.CMD_READER_FREE);

            msg.setData(bundle);
            msg.replyTo = localMessenger;
            try {
                saupService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Detach our existing connection.
            if(localContext != null) {
                localContext.unbindService(saupConnection);
            }
            isBound = false;
        }
    }


    /**
     * Receive message from SpringCard Android USB PCSC service
     */
    static class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();

                /* check for error code */
            switch (data.getInt("error")) {

                case SpringCardPCSC.ERROR_NO_READER: {
                    Toast.makeText(localContext, "Error : no reader", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_NO_READER");
                }
                break;

                case SpringCardPCSC.ERROR_WRONG_APDU_LENGTH: {
                    Toast.makeText(localContext, "Error : wrong APDU length", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_WRONG_APDU_LENGTH");
                }
                break;

                case SpringCardPCSC.ERROR_INVALID_APDU: {
                    Toast.makeText(localContext, "Error : invalid APDU", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_INVALID_APDU");
                }
                break;

                case SpringCardPCSC.ERROR_CARD_COMMUNICATION: {
                    Toast.makeText(localContext, "Error : card communication error", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_CARD_COMMUNICATION");
                }
                break;

                case SpringCardPCSC.ERROR_NO_ERROR:
                default: {
                    Log.d(TAG, "ERROR_NO_ERROR");
                }
            }

            Log.d(TAG, "handleMessage" + data.getInt("command"));

            switch (data.getInt("command")) {

                /* ask for a reader */
                case SpringCardPCSC.CMD_READER_STATE: {

                    remoteCardTerminal.setName(data.getString("reader"));
                    if (data.getInt("error") == SpringCardPCSC.ERROR_NO_READER) {
                        remoteCardTerminal.atr = null;
                        remoteCardTerminal.there_is_a_card = false;
                        callback.onTerminalRemoved(remoteCardTerminal);
                    } else {
                        callback.onTerminalInstalled(remoteCardTerminal);
                    }
                }
                break;

                /* card changed on reader */
                case SpringCardPCSC.CMD_CARD_STATE: {
                    remoteCardTerminal.setName(data.getString("reader"));

                    if (data.getInt("error") == SpringCardPCSC.ERROR_NO_READER) {
                        remoteCardTerminal.atr = null;
                        remoteCardTerminal.there_is_a_card = false;
                        callback.onTerminalRemoved(remoteCardTerminal);
                    } else {
                        if(data.getString("card").equals("Card present")) {

                            if(data.getString("atr").length()>=6) {
                                String atr = data.getString("atr").substring(6);
                                byte[] cmd = hexStringToByteArray(atr.replaceAll("\\s+", ""));
                                remoteCardTerminal.setATR(new ATR(cmd));
                            }

                            callback.onCardInserted(remoteCardTerminal);
                        } else {
                            callback.onCardRemoved(remoteCardTerminal);
                        }
                    }
                }
                break;

                /* RAPDU received */
                case SpringCardPCSC.CMD_RECV_RAPDU: {
                    String rapdu = data.getString("rapdu");
                    if (rapdu != null) {

                        ResponseAPDU reply = new ResponseAPDU(rapdu.getBytes());

                        Card card = remoteCardTerminal.connect("*");
                        CardChannel cardChannel= card.getBasicChannel();

                        callback.onResponseAPDU(cardChannel, reply);
                    }
                }
                break;

            }

            super.handleMessage(msg);
        }
    };


    /**
     * Hexadecimal String to Byte Array converter
     *
     * @param s string to transform
     * @return hexadecimal byte array
     */
    static public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int cpt = 0; cpt < len; cpt += 2) {
            data[cpt / 2] = (byte) ((Character.digit(s.charAt(cpt), 16) << 4)
                    + Character.digit(s.charAt(cpt+1), 16));
        }
        return data;
    }


    /**
     * Byte Array to Hexadecimal String converter
     * @param bytes
     * @return
     */
    public static String ByteArrayTohexString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
