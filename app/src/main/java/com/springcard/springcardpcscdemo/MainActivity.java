package com.springcard.springcardpcscdemo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    /* watermark for logging */
    private static final String     TAG                         = "Client MainActivity";

    /* COSMETIC */

    /* widget references */
    private TextView                productName_textView;
    private TextView                productState_textView;
    private TextView                ATR_textView;
    private TextView                logText;
    private TextView                goText;
    private View                    RunButton_View;

    /* Status items */
    private String                  product_string              = "No reader";
    private String                  card_string                 = "Card absent";
    private String                  atr_string                  = "ATR : ";


    /* COMMUNICATION PART */

    /* output messenger used to talk with the running service */
    final Messenger                 localMessenger              = new Messenger(new IncomingHandler());

    /* input messenger used to catch answer from the running service */
    Messenger                       saupService                 = null;

    /* service connection flag */
    boolean                         isSaupBound                 = false;


    /**
     * Create new activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* store display elements "address" */
        productName_textView = (TextView) findViewById(R.id.productName_textView);
        productState_textView = (TextView) findViewById(R.id.productState_textView);
        ATR_textView = (TextView) findViewById(R.id.ATR_textView);
        logText = (TextView) findViewById(R.id.operationResult_editText);
        goText = (TextView) findViewById(R.id.operationText);
        RunButton_View = (View) findViewById(R.id.runButton);

        /* setup saup */
        saupSetup();
    }


    /**
     * Destroy this activity
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        /* We must clean connexion with SpringCard Android USB PCSC service */
        if (isSaupBound) {

            /******************************/
            /* free this reader           */
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putInt("command", SpringCardPCSCCli.CMD_READER_FREE);

            msg.setData(bundle);
            msg.replyTo = localMessenger;
            try {
                saupService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Detach our existing connection.
            unbindService(saupConnection);
            isSaupBound = false;
        }
    }


    /**
     * Restore this activity
     */
    @Override
    protected void onResume() {
        super.onResume();

        /* setup saup */
        saupSetup();
    }


    /**
     * Check if service package is present or not and connect to if available
     */
    public void saupSetup() {

        /* check if service is available */
        if (!SpringCardPCSCCli.isPackageInstalled(this)) {
            Toast.makeText(getApplicationContext(), "com.springcard.android_usb_pcsc.service is not present on this system, please install it :)", Toast.LENGTH_LONG).show();
        } else if (!isSaupBound) {
            /* Connect to SpringCard Android USB PCSC service */
            /* this must be done before sending command to the reader */
            Toast.makeText(getApplicationContext(), "com.springcard.android_usb_pcsc.service found, connecting ...", Toast.LENGTH_LONG).show();

            /* bind to SpringCard Android USB PCSC service */
            Intent intent = new Intent("com.springcard.android_usb_pcsc.service");
            intent.setPackage("com.springcard.android_usb_pcsc.service");
            bindService(intent, saupConnection, Context.BIND_AUTO_CREATE);
        }
    }


    /**
     * Request access connection/disconnection callback
     */
    private ServiceConnection saupConnection = new ServiceConnection() {

        /* service connected */
        public void onServiceConnected(ComponentName className, IBinder service) {
            saupService = new Messenger(service);
            isSaupBound = true;

            /******************************/
            /* ask for reader information */
            Message msg = Message.obtain();

            Bundle bundle = new Bundle();
            bundle.putInt("command", SpringCardPCSCCli.CMD_READER_STATE);

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
            bundle.putInt("command", SpringCardPCSCCli.CMD_CARD_STATE);

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
            isSaupBound = false;
        }
    };


    /**
     * Receive message from SpringCard Android USB PCSC service
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();

            /* check for error code */
            switch(data.getInt("error")) {

                case SpringCardPCSCCli.ERROR_NO_READER : {
                    Toast.makeText(getApplicationContext(), "Error : no reader", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_NO_READER");
                } break;

                case SpringCardPCSCCli.ERROR_WRONG_APDU_LENGTH : {
                    Toast.makeText(getApplicationContext(), "Error : wrong APDU length", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_WRONG_APDU_LENGTH");
                } break;

                case SpringCardPCSCCli.ERROR_INVALID_APDU : {
                    Toast.makeText(getApplicationContext(), "Error : invalid APDU", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_INVALID_APDU");
                } break;

                case SpringCardPCSCCli.ERROR_CARD_COMMUNICATION : {
                    Toast.makeText(getApplicationContext(), "Error : card communication error", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "ERROR_CARD_COMMUNICATION");
                } break;

                case SpringCardPCSCCli.ERROR_NO_ERROR :
                default :{
                    Log.d(TAG, "ERROR_NO_ERROR");
                }
            }


            switch(data.getInt("command")) {

                /* ask for a reader */
                case SpringCardPCSCCli.CMD_READER_STATE: {

                    product_string = data.getString("reader");
                    if(data.getInt("error") == SpringCardPCSCCli.ERROR_NO_READER) {
                        atr_string =  "ATR : ";
                        card_string = "Card absent";
                    }
                    /* update display */
                    new updateDisplayTask().execute(product_string, card_string, atr_string);

                } break;

                /* card changed on reader */
                case SpringCardPCSCCli.CMD_CARD_STATE: {
                    atr_string = data.getString("atr");
                    product_string = data.getString("reader");
                    card_string = data.getString("card");

                    /* update display */
                    new updateDisplayTask().execute(product_string, card_string, atr_string);

                } break;

                 /* RAPDU received */
                case SpringCardPCSCCli.CMD_RECV_RAPDU: {
                    String rapdu = data.getString("rapdu");
                    if(rapdu != null) {
                        /* update display */
                        new updateResultTask().execute("> " + rapdu + "\n", "", "");
                    }
                } break;

            }

            Log.d(TAG, "handleMessage");
        }
    }


    /**
     * Run all APDU commands on the Card and display the result
     *
     * @param view current view used
     */
    public void connectButtonOnClick(View view) {
        /* check if we are connected */
        if (!isSaupBound) {
            Toast.makeText(getApplicationContext(), "You must be connected", Toast.LENGTH_LONG).show();
            saupSetup();
            return;
        }

        Message msg = Message.obtain();

        Bundle bundle = new Bundle();
        bundle.putInt("command", SpringCardPCSCCli.CMD_READER_STATE);

        msg.setData(bundle);
        msg.replyTo = localMessenger;
        try {
            saupService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Run all APDU commands on the Card and display the result
     *
     * @param view current view used
     */
    public void runButtonClick(View view) {

        /* clear result box */
        new updateResultTask().execute("CLEAR", "", "");

        Log.d(TAG, "runButtonClick");

        /* list all APDU commands */
        String[] separated = goText.getText().toString().split("\n");

        /* for each APDU found, sent it to the card */
        for (int i = 0; i < separated.length; i++) {

            /* Create a new command to the reader */
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();

            /* specify message type */
            bundle.putInt("command", SpringCardPCSCCli.CMD_SEND_APDU);

            /* apdu contains an apdu to execute on the card */
            bundle.putString("apdu",separated[i]);

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
    }


    /*****************/
    /** VISUAL PART **/
    /*****************/


    /**
     * Update product name, card and atr
     */
    private class updateDisplayTask extends AsyncTask<String,String ,String> {
        private Handler hdl;

        @Override
        protected void onPreExecute() {
            hdl = new Handler();
        }

        @Override
        protected String doInBackground(final String... str) {
            hdl.post(new Runnable() {
                public void run() {
                    productName_textView.setText(str[0]);
                    productState_textView.setText(str[1]);
                    ATR_textView.setText(str[2]);
                    if (str[1]!=null && str[1].equals("Card present")) {
                        RunButton_View.setEnabled(true);
                    } else {
                        RunButton_View.setEnabled(false);
                    }
                }
            });

            return "Done";
        }
    }


    /**
     * Update result text
     */
    private class updateResultTask extends AsyncTask<String,String ,String> {
        private Handler hdl;

        @Override
        protected void onPreExecute() {
            hdl = new Handler();
        }

        @Override
        protected String doInBackground(final String... str) {
            hdl.post(new Runnable() {
                public void run() {
                    if(str[0].equals("CLEAR")) {
                        logText.setText("");
                    } else {
                        logText.append(str[0]);
                    }
                    logText.refreshDrawableState();
                }
            });

            return "Done";
        }
    }


    /**
     * call Ludovic Rousseau's ATR parser online
     * @param view
     */
    public void onClickATR(View view) {
        if(ATR_textView.getText().toString().length()>6) {
            String url = "https://smartcard-atr.appspot.com/parse?ATR=" +
                    ATR_textView.getText().toString().substring(6).replace(" ", "");

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            Log.d(TAG, "ATR Analysis !");

            finish();
        }
    }

}
