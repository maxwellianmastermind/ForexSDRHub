package com.terrajolt.forexsdrhub;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Chronometer;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by William on 4/28/2016.
 */
public class ForexOrderService extends Service {

    //The Amazon Web Services server url that the DTCC purportedly uses to store slice reports
    private final String BASE_REPORT_URL = "https://kgc0418-tdw-data-0.s3.amazonaws.com/slices/";

    private String reportFilename_Slice;
    private final String REPORT_FILENAME_SLICE_BASE = "SLICE_FOREX_";

    private Chronometer synchrotronFluxClock;
    private Handler sliceHandler;
    private Runnable sliceRunnable;

    private SQLiteDatabase theOrderDatabase;
    private ForexOrderDbHelper mDbHelper;
    private int sliceNumber = 1;
    private boolean retryFile = false;
 //   private WeakReference<MainActivity> theAct;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    //    synchrotronFluxClock = new Chronometer(this);
    //    synchrotronFluxClock.setBase(Calendar.getInstance().getTimeInMillis());
        final Service theServ = this;
        mDbHelper = new ForexOrderDbHelper(getApplicationContext());



        sliceHandler = new Handler();
        sliceHandler.postDelayed(sliceRunnable, 1000);
        sliceRunnable = new Runnable() {
            @Override
            public void run() {
      /* do what you need to do */
                //        Toast.makeText(getApplicationContext(),"Testing the start of service runnable",Toast.LENGTH_LONG).show();
                StringBuilder sliceBuild = new StringBuilder();
                //reportFilename_Slice = "SLICE_FOREX_2016_04_29_110.zip";
                Calendar currentCal = Calendar.getInstance();
                /*
                Since the AWS server is calibrated to GMT, we'll have to adjust when the service
                begins to poll for new slices by 4 hours or so.
                */
                if (currentCal.get(Calendar.HOUR_OF_DAY) >= 20) {
                    currentCal.set(Calendar.DAY_OF_YEAR, currentCal.get(Calendar.DAY_OF_YEAR) + 1);
                }
                String formattedDate = new SimpleDateFormat("yyyy_MM_dd").format(currentCal.getTime());

                reportFilename_Slice = sliceBuild.append(REPORT_FILENAME_SLICE_BASE).append(formattedDate+"_").append(Integer.toString(sliceNumber)+".zip").toString();
                /*
                TODO: Investigate making the asynctask static, and then rework its guts accordingly
                for all the fucking non-static contexts in there.

                TODO NEARLY COMPLETE?
                 */
                MainActivity.DownloadParseSRFileTask dpsrfTask = new MainActivity.DownloadParseSRFileTask((ForexOrderService)theServ);
                dpsrfTask.execute(reportFilename_Slice);


      /* and here comes the "trick" */
                if (!retryFile) {
                    sliceNumber++;
 //                   retryFile = false;
                }
                //reportFilename_Slice = REPORT_FILENAME_SLICE_BASE+"2016_04_29"+"_"+Integer.toString(sliceNumber)+".zip";

                sliceHandler.postDelayed(this, 60000);
            }
        };
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ForexOrderDbHelper getmDbHelper() {
        return mDbHelper;
    }

    public void setRetry(boolean buh) {
        retryFile = buh;
    }

}
