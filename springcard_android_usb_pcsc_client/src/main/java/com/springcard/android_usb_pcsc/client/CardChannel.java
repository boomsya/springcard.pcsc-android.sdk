package com.springcard.android_usb_pcsc.client;

/**
 * Created by matthieu.b on 15/06/2015.
 */
public class CardChannel {
    public void transmit(CommandAPDU command) {
        SpringCardPCSC.sendCommandAPDU(command);
    }
}
