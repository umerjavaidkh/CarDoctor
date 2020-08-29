package com.github.pires.obd.reader.ads;

import com.github.pires.obd.reader.ODBII_Reader;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;


/**
 * Created by UMER on 5/12/2016.
 */
public class ShowAd {

    public static InterstitialAd mInterstitialAd;



    public static void createAd()
    {
       
    }


    private static void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()

                .build();

        mInterstitialAd.loadAd(adRequest);



    }

    public static void showAd() {

       /* try {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }catch (Exception ex){}*/
    }
}
