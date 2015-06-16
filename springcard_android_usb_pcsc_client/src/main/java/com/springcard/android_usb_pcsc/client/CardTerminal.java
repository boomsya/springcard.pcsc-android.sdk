/*
 * CardTerminal.java
 *
 * specific code format to follow Oracle's javax.smartcardio
 * Created by matthieu.b on 05/05/2015.
 */
package com.springcard.android_usb_pcsc.client;
import android.content.Context;
import android.util.Log;


/**
 * A Smart Card terminal, sometimes referred to as a Smart Card Reader.
 * A CardTerminal object can be obtained by calling
 *
 * <p>Note that physical card readers with slots for multiple cards are
 * represented by one <code>CardTerminal</code> object per such slot.
 *
 */
public class CardTerminal {

    /* watermark for logging */
    private static final String     TAG                         = "CardTerminal";

    /* slot used by this CardTerminal */
    private int slotNumber                                      = 0;

    /* current sequence number */
    private int sequenceNumber                                  = 0;

    public String                   product_name                = "";

    /* is there a card on the reader ? */
    public Boolean                 there_is_a_card             = false;

    /* current ATR */
    public ATR                     atr                         = null;

    public Context localContext                                = null;

    public CardTerminal(Context localContext) {
        this.localContext = localContext;
    }


    /**
     * get reader's name
     *
     * @return the unique name of this terminal
     *
     */
    public String getName() {
        return this.product_name;
    }


    /**
     * set reader's name
     *
     */
    public void setName(String name) {
        this.product_name = name;
    }


    /**
     * Establishes a connection to the card.
     * If a connection has previously established using
     * the specified protocol, this method returns the same Card object as
     * the previous call.
     *
     * @param cnxString the protocol to use. "*" to
     *   connect using any available protocol.
     *
     * @return a Card object
     *
     */
    public Card connect(String cnxString) {
        Log.d(TAG, "connect using protocol " + cnxString);
        return new Card();
    }


    /**
     * Store ATR for this slot
     *
     * @param atr new atr to store
     *
     */
    public void setATR(ATR atr) {
        Log.d(TAG, "setATR ");
        this.atr = atr;
    }


    /**
     * Get ATR for this slot
     *
     * @return current slot's ATR (if any)
     */
    public ATR getATR() {
        Log.d(TAG, "getATR ");
        return this.atr;
    }


    /**
     * Is the a card on this reader (slot) ?
     *
     * @return true if a card is present
     *
     */
    public Boolean isCardPresent() {
       return this.there_is_a_card;
    }

}
