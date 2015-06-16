package com.springcard.springcardpcscdemo;

import android.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.springcard.android_usb_pcsc.client.*;

public class MainActivity extends Activity implements SpringCardPCSC.Client {

    /* watermark for logging */
    private static final String     TAG                         = "Client MainActivity";

    /* COSMETIC */

    /* widget references */
    private TextView        productName_textView;
    private TextView        productState_textView;
    private TextView        ATR_textView;
    private TextView        logText;
    private TextView        goText;
    private View            RunButton_View;

    /* Status items */
    static private String   product_string = "No reader";
    static private String   card_string = "Card absent";
    static private String   atr_string = "ATR : ";

    /* connected card channel */
    private CardChannel channel = null;

    /* current scenario position */
    private int             curScenarioEntry = 0;

    /* max scenario */
    private int             numScenarioEntries = 0;

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

        /* check if service is available [ALWAYS!]*/
        if (!SpringCardPCSC.isServiceAvailable(this)) {

            Toast.makeText(getApplicationContext(), "com.springcard.android_usb_pcsc.service is not present on this system, please install it :)", Toast.LENGTH_LONG).show();

        }
    }


    /**
     * Destroy this activity
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        /* unbind from the service */
        SpringCardPCSC.unBindToService();
    }


    /**
     * Resume this activity
     */
    @Override
    public void onResume() {
        super.onResume();

        if(SpringCardPCSC.isServiceAvailable(this) && !SpringCardPCSC.isBound) {
            /* Connect to SpringCard Android USB PCSC service */
            /* this must be done before sending command to the reader */
            Toast.makeText(getApplicationContext(), "com.springcard.android_usb_pcsc.service found, connecting ...", Toast.LENGTH_LONG).show();

            /* bind to the service */
            SpringCardPCSC.bindToService(this);
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

        /* list all APDU commands */
        String[] separated = goText.getText().toString().split("\n");

        /* store command list length */
        numScenarioEntries = separated.length;

        if(numScenarioEntries>0) {
            /* send first apdu in list */
            curScenarioEntry = 0;
            sendCommand(0);
        }
    }


    /**
     * send an APDU from the list
     * @param cmdNumber
     */
    void sendCommand(int cmdNumber) {
        CommandAPDU command = null;

        /* list all APDU commands */
        String[] separated = goText.getText().toString().split("\n");

        /* send APDU */
        byte[] cmd = SpringCardPCSC.hexStringToByteArray(separated[cmdNumber].replaceAll("\\s+", ""));
        try {
            command = new CommandAPDU(cmd);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "< Invalid APDU\n");
        }

        /* done with this message, prepare for next one */
        Log.d(TAG, "Sending APDU n "+cmdNumber);
        curScenarioEntry++;

        /* now send the command */
        if(channel!=null && command != null) {
            channel.transmit(command);
        }

    }



    /*****************/
    /** CALLBACK PART **/
    /*****************/

    @Override
    public void onTerminalInstalled(CardTerminal terminal) {
        String ct = (terminal.isCardPresent() ? "Card present" : "Card absent");
        String atr = (terminal.getATR() == null ? "ATR : " : "ATR : "+terminal.getATR().toString());
        new updateDisplayTask().execute(terminal.getName(),ct, atr);
    }


    @Override
    public void onTerminalRemoved(CardTerminal terminal) {
        new updateDisplayTask().execute(terminal.getName(),"Card absent", "ATR : ");
    }


    @Override
    public void onCardInserted(CardTerminal terminal) {
        Card card = terminal.connect("*");
        this.channel = card.getBasicChannel();


        /* cosmetic */
        String ct = "Card present";
        String atr_str = "ATR : ";

        ATR atr = terminal.getATR();
        if(atr != null) {
            int atr_len = atr.getBytes().length;

            for (int yyy = 0; yyy < atr_len; yyy++) {
                atr_str += String.format("%02X", atr.getBytes()[yyy]) + " ";
            }
        }
        new updateDisplayTask().execute(terminal.getName(),ct, atr_str);
        /* end cosmetic */
    }


    @Override
    public void onCardRemoved(CardTerminal terminal) {
        new updateDisplayTask().execute(terminal.getName(),"Card absent", "ATR : ");
    }


    @Override
    public void onResponseAPDU(CardChannel channel, ResponseAPDU response) {
        /* update display */
        String apdu_str;

        if(response.getBytes().length>0) {
            apdu_str = new String(response.getBytes());
        } else {
            apdu_str = "";
        }

        new updateResultTask().execute("> " + apdu_str + "\n", "", "");

        /* is there others APDU to send ? */
        if(curScenarioEntry<numScenarioEntries) {
            Log.d(TAG, "Sending another APDU n "+curScenarioEntry);
            sendCommand(curScenarioEntry);
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
