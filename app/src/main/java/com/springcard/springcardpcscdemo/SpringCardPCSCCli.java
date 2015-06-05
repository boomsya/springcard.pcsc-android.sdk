package com.springcard.springcardpcscdemo;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by matthieu.b on 04/06/2015.
 */
public class SpringCardPCSCCli {
    public static final String  packageName             = "com.springcard.android_usb_pcsc.service";

    /* command list */
    public static final int               CMD_READER_STATE            = 1;
    public static final int               CMD_CARD_STATE              = 2;
    public static final int               CMD_SEND_APDU               = 3;
    public static final int               CMD_RECV_RAPDU              = 4;
    public static final int               CMD_READER_FREE             = 5;

    /* error list */
    public static final int               ERROR_NO_ERROR              = 10;
    public static final int               ERROR_NO_READER             = 11;
    public static final int               ERROR_WRONG_APDU_LENGTH     = 12;
    public static final int               ERROR_INVALID_APDU          = 13;
    public static final int               ERROR_CARD_COMMUNICATION    = 14;

    /* check if service is available on this device */
    public static boolean isPackageInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }



}
