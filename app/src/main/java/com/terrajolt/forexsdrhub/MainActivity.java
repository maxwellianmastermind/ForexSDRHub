package com.terrajolt.forexsdrhub;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CalendarView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.tyczj.extendedcalendarview.CalendarProvider;
import com.tyczj.extendedcalendarview.Day;
import com.tyczj.extendedcalendarview.Event;
import com.tyczj.extendedcalendarview.ExtendedCalendarView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //CurrencyLayer API Key. Don't Misplace It!!!
    private static final String CURRENCY_LAYER_API_KEY = "dd6d876e6d0041145c0308f59f7ccecc";

    //CurrencyLayer url for accessing most recent spot prices
    private final String BASE_CURRENCY_URL = "http://apilayer.net/api/";
    //http://www.apilayer.net/api/live?access_key=dd6d876e6d0041145c0308f59f7ccecc for access

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    //The Amazon Web Services server url that the DTCC purportedly uses to store slice reports
    private final String BASE_REPORT_URL = "https://kgc0418-tdw-data-0.s3.amazonaws.com/slices/";

    private String reportFilename_Cumulative;// = "CUMULATIVE_FOREX_2016_03_18.zip";
    private String reportFilename_Slice;
    /*
    The Options Order Book, implemented as a Hashmap (for now), where the keys are order IDs on the
    slice reports and the values are ForexOrder objects which contain all the relevant data
     */
    private HashMap<Integer,ForexOrder> theOrderBook;

    /*
    The database stored locally on the device, which "absorbs" orders obtained from the foreground
    Activity (during user UI interactions such as the fetching of the cumulative report file) as well
    as the background Service yet to be implemented.
    */
    private SQLiteDatabase theOrderDatabase;
    private ForexOrderDbHelper mDbHelper;

    private ExtendedCalendarView forexOrderCalendar;
    private ProgressBar theProgressBar;

    //private String cumCSVFile;
    private String API_LINK;
    private FragmentManager fragMan;
    private AlertDialog.Builder builder;
    //private PopupWindow orderDisplay;

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        StringBuilder currencyURLMaker = new StringBuilder();
        API_LINK = currencyURLMaker.append(BASE_CURRENCY_URL).append("live?access_key=").append(CURRENCY_LAYER_API_KEY).toString();

        forexOrderCalendar = (ExtendedCalendarView)findViewById(R.id.orderBoardCalendar);

       fragMan = getSupportFragmentManager();
        final Activity theAct = this;

        forexOrderCalendar.setOnDayClickListener(new ExtendedCalendarView.OnDayClickListener() {

            @Override
            public void onDayClicked(AdapterView<?> adapter, View view, int position, long id, Day day) {
                //Log.v("TEST LISTENER",Integer.toString(position)+" "+Long.toString(id)+" "+day.toString());
                StringBuilder evSb = new StringBuilder();
                ArrayList<Event> daysOrders = day.getEvents();
                final Day theDay = day;
                for (Event ev : daysOrders) {
                    //Log.v("TEST EVENTS",ev.getDescription());
                    //                  orderDisplay = new PopupWindow(getApplicationContext());
                    //                 orderDisplay.showAtLocation(findViewById(R.id.currentSpotPrice), Gravity.BOTTOM,10,10);
                    //               orderDisplay.update(50,50,300,300);
                    //             TextView orderWords = new TextView(getApplicationContext());
                    //           orderWords.setText((CharSequence)ev.getDescription());
                    //         orderDisplay.setContentView(orderWords);
                    //OrderFragment orderFrag = new OrderFragment();
                    //orderFrag.getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    //orderFrag.show(fragMan,"Derp");
                    //Snackbar.make(view, ev.getDescription(), Snackbar.LENGTH_LONG)
                    //       .setAction("Action", null).show();
                    // 1. Instantiate an AlertDialog.Builder with its constructor
                    evSb.append(ev.getLocation()).append(" : \n").append(ev.getDescription());
                }
                String evStr = evSb.toString();
                builder = new AlertDialog.Builder(theAct);

// 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage(evStr)
                        .setTitle("Forex Orders for " + (day.getMonth() + 1) + "/" + day.getDay())
                        .setPositiveButton("Download Cumulative Slice File", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                StringBuilder downloadURLMaker = new StringBuilder();
                                Calendar currentCal = Calendar.getInstance();
                                String formattedDate;
        /*
        The AWS server updates with the latest cumulative file every evening at 8:04 PM, local time.
        So, unless it's past that time of day, pull yesterday's file instead
        */
                                if ((currentCal.get(Calendar.HOUR_OF_DAY) < 20) || (currentCal.get(Calendar.HOUR_OF_DAY) == 20 && currentCal.get(Calendar.MINUTE) < 5)) {
                                    currentCal.set(Calendar.DAY_OF_YEAR, currentCal.get(Calendar.DAY_OF_YEAR) - 1);
                                }
                                //formattedDate = new SimpleDateFormat("yyyy_MM_dd").format(currentCal.getTime());
                                formattedDate = new SimpleDateFormat("yyyy_MM_dd").format(new Date(theDay.getYear() - 1900, theDay.getMonth(), theDay.getDay()));
                                Log.v("tag", formattedDate);
                                reportFilename_Cumulative = downloadURLMaker.append("CUMULATIVE_FOREX_" + formattedDate + ".zip").toString();//.append(formattedDate).append(".zip").toString();
                                DownloadParseSRFileTask dpsrfTask = new DownloadParseSRFileTask((MainActivity)theAct);
                                dpsrfTask.execute(reportFilename_Cumulative);
                            }
                        });


// 3. Get the AlertDialog from create()
                AlertDialog dialog = builder.create();
                dialog.show();
                    /*
                    String[] projection = {
                            CalendarProvider.EVENT,
                            CalendarProvider.LOCATION,
                            CalendarProvider.DESCRIPTION,
                            CalendarProvider.START,
                            CalendarProvider.END,
                            CalendarProvider.START_DAY,
                            CalendarProvider.END_DAY,
                            CalendarProvider.COLOR

                    };
                    Cursor c = getContentResolver().query(CalendarProvider.CONTENT_URI,projection,null,null,null);
                    c.move(position);
                    String descript = c.getString(c.getColumnIndexOrThrow(CalendarProvider.DESCRIPTION));
                    Log.v("TAG", descript);
                    */
            }
        });

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                TextView spotPriceDisplay = (TextView) findViewById(R.id.currentSpotPrice);

                GrabSpotPriceDataTask gspdTask = new GrabSpotPriceDataTask();
                gspdTask.execute("SGD");


            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        theOrderBook = new HashMap<Integer,ForexOrder>();

        mDbHelper = new ForexOrderDbHelper(getApplicationContext());

        /*
        Booting up the service that will continuously download new slices as they appear and notify
        me of any outstanding orders
        */
        startService(new Intent(this,ForexOrderService.class));


        //reportFilename_Cumulative = downloadURLMaker.append("CUMULATIVE_FOREX_2016_04_08.zip").toString();
        //reportFilename_Slice = downloadMaker.append("SLICE_FOREX_").append(formattedDate).append("????????");

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.download_cum_file) {
            /* TODO: COMPLETE A/O 4 Apr 2016
            * Specify input parameter configuration for the background task that refers to the
            * filename of the slice report file to download, given the current date and time or
            * whatever
            */
            /* TODO:
            Address aforementioned TODO erroneously marked as COMPLETE
             */
            DownloadParseSRFileTask dpsrfTask = new DownloadParseSRFileTask(this);
            dpsrfTask.execute();
        }
        if (id == R.id.read_database) {
            theOrderDatabase = mDbHelper.getReadableDatabase();
            String[] projection = {
                    ForexOrdersContract.OrderEntry._ID,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_ENTRY_ID,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_EXEC_TIME,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_TYPE,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY1,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY2,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL1,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL2,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_STRIKE_PRICE,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_CURR,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_PREMIUM,
                    ForexOrdersContract.OrderEntry.COLUMN_NAME_EXPIRATION
            };

            Cursor c = theOrderDatabase.query(ForexOrdersContract.OrderEntry.TABLE_NAME,
                    projection,null,null,null,null,null);
            c.moveToFirst();
            float premium = c.getFloat(c.getColumnIndexOrThrow(ForexOrdersContract.OrderEntry.COLUMN_NAME_PREMIUM));
            Log.v("TAG", Float.toString(premium));
        }
        if (id == R.id.delete_database) {
            theOrderDatabase.delete(ForexOrdersContract.OrderEntry.TABLE_NAME,null,null);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public class GrabSpotPriceDataTask extends AsyncTask<String,Void,Float> {

        private final String LOG_TAG = GrabSpotPriceDataTask.class.getSimpleName();

        @Override
        protected Float doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String currencyJsonStr = null;

            try {
                final String CURRENCY_PARAM = "currencies";
                final String FORMAT_PARAM = "format";

                Uri builtUri = Uri.parse(API_LINK).buildUpon().appendQueryParameter(CURRENCY_PARAM,"SGD")
                        .appendQueryParameter(FORMAT_PARAM,"1")
                        .build();

                URL currencyURL = new URL(builtUri.toString());

                connection = (HttpURLConnection) currencyURL.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return new Float(0.0);
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return new Float(0.0);
                }
                currencyJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Currency JSON String: " + currencyJsonStr);

            }  catch (Exception e) {
                Log.e(LOG_TAG,"Couldn't do something with the Currency Layer");
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return new Float(0.0);
        }
    }


    public static class DownloadParseSRFileTask extends AsyncTask<String,Integer,Void> {

        private final String LOG_TAG = DownloadParseSRFileTask.class.getSimpleName();
        private Toast toaster;
        private WeakReference<MainActivity> theAct;
        private WeakReference<ForexOrderService> theServ;
        private String cumCSVFile;
        private boolean serviceRunning;
        private final String BASE_REPORT_URL = "https://kgc0418-tdw-data-0.s3.amazonaws.com/slices/";

        public DownloadParseSRFileTask(MainActivity aty) {
            theAct = new WeakReference<MainActivity>(aty);
            serviceRunning = false;
        }

        public DownloadParseSRFileTask(ForexOrderService serv) {
            theServ = new WeakReference<ForexOrderService>(serv);
            serviceRunning = true;
        }

        @Override
        protected void onPreExecute() {
            //theProgressBar = (ProgressBar)findViewById(R.id.sliceProgressBar);
            //theProgressBar.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //theProgressBar.setProgress(values[0]);
            Log.v("TAG",values[0].toString());

            if (!serviceRunning) {

                switch (values[0]) {
                    case 0:
                        //    toaster = Toast.makeText(theServ.get(),"",Toast.LENGTH_SHORT);
                        //} else {
                        toaster = Toast.makeText(theAct.get(), "", Toast.LENGTH_SHORT);
                        toaster.setText("Starting Orderboard Build");
                        toaster.setDuration(Toast.LENGTH_SHORT);
                        toaster.show();
                        break;
                    case 1:
                        //toaster.cancel();
                        //Toast.makeText(getApplicationContext(),"Downloaded Slice Report",Toast.LENGTH_LONG).show();
                        toaster.setText("Downloaded Slice Report");
                        toaster.show();
                        break;
                    case 2:
                        //Toast.makeText(getApplicationContext(),"Unzipped Report Archive",Toast.LENGTH_LONG).show();
                        toaster.setText("Unzipped Report Archive");
                        toaster.show();
                        break;
                    case 3:
                        //Toast.makeText(getApplicationContext(),"Parsing Orders...Please Wait",Toast.LENGTH_LONG).show();
                        toaster.setText("Parsing Orders... Please Wait");
                        toaster.setDuration(Toast.LENGTH_LONG);
                        toaster.show();
                        break;
                    case 4:
                        //Toast.makeText(getApplicationContext(),"Orderboard Build Finished",Toast.LENGTH_LONG).show();
                        toaster.setText("Orderboard Build Finished");
                        toaster.show();
                        break;

                }
            }

        }

        @Override
        protected Void doInBackground(String... params) {

            int count;

            try {


                Uri builtUri = Uri.parse(BASE_REPORT_URL).buildUpon().appendPath(params[0])
                        .build();

                URL url = new URL(builtUri.toString());

                //             Log.v(LOG_TAG,url.toString());


                publishProgress(0);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("HEAD");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    connection.connect();
                }  else {
                    return null;
                }

                int lengthOfFile = connection.getContentLength();
                Log.d(LOG_TAG, "Length of file: " + lengthOfFile);

                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream("/sdcard/" + params[0]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    //publishProgress(""+(int)((total*100)/lengthOfFile));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                publishProgress(1);

                if (serviceRunning) {
                    theServ.get().setRetry(false);
                }
                //Now that we have the zipped file, we will proceed to unzip it!
                unzip("/sdcard/" + params[0]);

                publishProgress(2);
                //Ok, next step now is to parse through the spreadsheet and build that order book!
                //<Insert Android .csv code here>
                //FileInputStream csvStream = new FileInputStream(getExternalFilesDir(null).toString() + "/" + cumCSVFile);
                List<String[]> csvList = null;
                if (serviceRunning) {
                    csvList = read((theServ.get().getExternalFilesDir(null).toString() + "/" + cumCSVFile));
                } else {
                    csvList = read((theAct.get().getExternalFilesDir(null).toString() + "/" + cumCSVFile));
                }
                if (csvList.size() > 0) {
                    //StringBuilder sb = new StringBuilder();

                    /*
                    In this conditional, we will run down each row of the csvList and construct
                    Forex Orders accordingly, based on sufficient criteria (e.g., high option premium,
                    expiration date close in temporal proximity to the current day)

                    Actually, the expiration date criterion will be relaxed, in favor of gathering
                    as many orders as possible to increase opportunities for playing swing events.
                    So we will extend the horizon of our order board, so to speak, out to at least
                    several months.
                    */
                    //String[] testarr = csvList.get(20);
                    publishProgress(3);
                    for (int i = 1; i < csvList.size(); i++) {
                        //sb.append(testarr[i]);
                        String[] orderLine = csvList.get(i);
                        /*
                        for (int k = 0; k < orderLine.length; k++) {
                            Log.v(LOG_TAG, orderLine[k].toString());
                        }
                        */
                        /*
                        We know the first element in this array is the dissemination_id, which
                        will comprise the key for the HashMap. But we want to make sure that this
                        order line hasn't already been CANCELed by another order ID, which is
                        indicated in the ACTION column
                        Backup, though: we don't even care about ANY order below a certain value
                        */
                        float optionPremium = 0;
                        if (!orderLine[37].isEmpty()) {
                            orderLine[37] = orderLine[37].replace(",","");
                            optionPremium = Float.parseFloat(orderLine[37]);
                        }
                        String optionCurrency = orderLine[36];
                        long notionalAmount1 = 0;
                        if (!orderLine[26].isEmpty()) {
                            orderLine[26] = orderLine[26].replace(",","").replace("+","");
                            notionalAmount1 = Long.parseLong(orderLine[26]);
                        }
                        long notionalAmount2 = 0;
                        if (!orderLine[27].isEmpty()) {
                            orderLine[27] = orderLine[27].replace(",","").replace("+","");
                            notionalAmount2 = Long.parseLong(orderLine[27]);
                        }

                        String notionalCurrency1 = orderLine[24];
                        String notionalCurrency2 = orderLine[25];

                        String taxonomy = orderLine[16];

                        /*
                        Log.v(LOG_TAG,Float.toString(optionPremium));
                        Log.v(LOG_TAG,optionCurrency);
                        Log.v(LOG_TAG, Long.toString(notionalAmount1));
                        Log.v(LOG_TAG, Long.toString(notionalAmount2));
                        */
                        if ((!taxonomy.equals("ForeignExchange:NDF") && !taxonomy.equals("ForeignExchange:ComplexExotic") &&
                        !taxonomy.equals("ForeignExchange:SimpleExotic:Digital")) &&
                                (!notionalCurrency1.equals("KRW") && !notionalCurrency1.equals("INR") &&
                                        !notionalCurrency1.equals("BRL") &&
                                        !notionalCurrency2.equals("KRW") && !notionalCurrency2.equals("INR") &&
                                        !notionalCurrency2.equals("BRL")) &&
                        ((optionPremium >= 500000 && !optionCurrency.equals("JPY")) ||
                                (optionPremium >= 50000000 && optionCurrency.equals("JPY")) ||
                                (notionalAmount1 >= 250000000 && (!notionalCurrency1.equals("JPY")) &&
                                        (notionalAmount2 >= 250000000 && !notionalCurrency2.equals("JPY"))))) {
                            //We made it past our first round of checks
                            String optionExpiration = "1970-01-01";
                            if (!orderLine[39].isEmpty()) {
                                optionExpiration = orderLine[39];
                            }
                            String optionFamily = orderLine[35];
                            //if (optionFamily.equals("EU"))
                            int disseminationID = Integer.parseInt(orderLine[0]);
                            int originalDissemID = 0;
                            if (!orderLine[1].isEmpty()) {
                                originalDissemID = Integer.parseInt(orderLine[1]);
                            }
                            String action = orderLine[2];
                            String executionTime = orderLine[3];
                            float strikePrice = 0;
                            if (!orderLine[33].isEmpty()) {
                                orderLine[33] = orderLine[33].replace(",","");
                                strikePrice = Float.parseFloat(orderLine[33]);
                            }
                            String optionType = orderLine[34];
                            Integer origDissemInt = new Integer(originalDissemID);
                            Integer dissemInt = new Integer(disseminationID);
                            if (!serviceRunning && (theAct.get().theOrderBook.containsKey(origDissemInt) && action.equals("CANCEL"))) {
                                theAct.get().theOrderBook.remove(origDissemInt);
                                continue;
                            }
                            else {
                                ForexOrder fxOrder = new ForexOrder(dissemInt,executionTime,notionalCurrency1,
                                        notionalCurrency2,notionalAmount1,notionalAmount2,strikePrice,
                                        optionType,optionCurrency,optionPremium,optionExpiration);
                                if (!serviceRunning) {
                                    theAct.get().theOrderBook.put(dissemInt, fxOrder);
                                }
                                Log.v(LOG_TAG,fxOrder.toString());

                                SQLiteDatabase theOrdDat;
                                if (serviceRunning) {
                                    theOrdDat = theServ.get().getmDbHelper().getWritableDatabase();
                                    NotificationCompat.Builder ordNotifBuilder = new NotificationCompat.Builder(theServ.get());
                                    ordNotifBuilder.setSmallIcon(R.drawable.ic_menu_send);
                                    ordNotifBuilder.setContentTitle("New Inbound Order!");
                                    ordNotifBuilder.setContentText(fxOrder.toString());

                                    NotificationManager mNotificationManager = (NotificationManager) theServ.get().getSystemService(Context.NOTIFICATION_SERVICE);

// notificationID allows you to update the notification later on.
                                    mNotificationManager.notify(9999, ordNotifBuilder.build());
                                } else {
                                    theAct.get().theOrderDatabase = theAct.get().mDbHelper.getWritableDatabase();
                                    theOrdDat = theAct.get().theOrderDatabase;
                                }

                                ContentValues values = new ContentValues();
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_ENTRY_ID, dissemInt);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_EXEC_TIME,executionTime);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_TYPE,optionType);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY1,notionalCurrency1);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY2,notionalCurrency2);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL1,notionalAmount1);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL2,notionalAmount2);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_STRIKE_PRICE,strikePrice);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_CURR,optionCurrency);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_PREMIUM,optionPremium);
                                values.put(ForexOrdersContract.OrderEntry.COLUMN_NAME_EXPIRATION,optionExpiration);

                                long newRowId;
                                if (serviceRunning) {
                                    newRowId = theOrdDat.insert(ForexOrdersContract.OrderEntry.TABLE_NAME,null,values);
                                    //theServ.setOrderDatabase(theOrdDat);
                                } else {
                                    newRowId = theAct.get().theOrderDatabase.insert(ForexOrdersContract.OrderEntry.TABLE_NAME, null, values);
                                }

                                values = new ContentValues();
                                values.put(CalendarProvider.COLOR, Event.COLOR_RED);
                                //values.put(CalendarProvider.DESCRIPTION, fxOrder.toString());
                                String theDescript = fxOrder.getOptionType()+"\nExecuted: "+fxOrder.getExecutionTimestamp()+",\n"+fxOrder.getCurrencyPair()+"\nNotional Amounts: "+
                                        fxOrder.getAmountPair()+"\nStrike Price: "+Float.toString(fxOrder.getStrikePrice())+"\nPremium: "+
                                        Float.toString(fxOrder.getOptionPremium())+"\nExpiration Date: "+ fxOrder.getOptionExpirationDate().toString()+"\n";
                                values.put(CalendarProvider.DESCRIPTION, theDescript);
                                values.put(CalendarProvider.LOCATION, fxOrder.getCurrencyPair());
                                values.put(CalendarProvider.EVENT, "New Forex Order");

                                Calendar cal = Calendar.getInstance();

                                cal.setTime(fxOrder.getOptionExpirationDate());
                                //Log.v(LOG_TAG, fxOrder.getOptionExpirationDate().toString());
                                TimeZone tz = TimeZone.getDefault();

                                values.put(CalendarProvider.START, cal.getTimeInMillis());
                                int startDayJulian = Time.getJulianDay(cal.getTimeInMillis(), TimeUnit.MILLISECONDS.toSeconds(tz.getOffset(cal.getTimeInMillis())));
                                values.put(CalendarProvider.START_DAY,startDayJulian);

                                values.put(CalendarProvider.END,cal.getTimeInMillis());
                                int endDayJulian = Time.getJulianDay(cal.getTimeInMillis(), TimeUnit.MILLISECONDS.toSeconds(tz.getOffset(cal.getTimeInMillis())));
                                values.put(CalendarProvider.END_DAY,endDayJulian);


                                if (!serviceRunning) {
                                    Uri uri = theAct.get().getContentResolver().insert(CalendarProvider.CONTENT_URI, values);
                                }
                            }
                        }
                        else {
                            continue;
                        }
                        //publishProgress(40+Math.round((60*i)/csvList.size()));


                    }
                    //Log.v(LOG_TAG, sb.toString());
                    publishProgress(4);
                }

            } catch (FileNotFoundException fe) {
                Log.e(LOG_TAG,"Couldn't find the file on the AWS. Trying again momentarily...");
                fe.printStackTrace();
                if (serviceRunning) {
                    theServ.get().setRetry(true);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG,"YOU DUN FUCKED UP");
                e.printStackTrace();
            }
            return null;
        }

        public void unzip(String zipFile) {
            try  {
                FileInputStream fin = new FileInputStream(zipFile);
                BufferedInputStream bin = new BufferedInputStream(fin);
                ZipInputStream zin = new ZipInputStream(bin);
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    Log.v("Decompress", "Unzipping " + ze.getName());

                    if(ze.isDirectory()) {
                        _dirChecker(ze.getName());
                    } else {
                        //Log.v(LOG_TAG,getFilesDir().toString()+"/"+ze.getName());
                        cumCSVFile = ze.getName();
                        FileOutputStream fout = null;
                        if (serviceRunning) {
                            fout = new FileOutputStream(theServ.get().getExternalFilesDir(null).toString()+"/"+ze.getName());
                        } else {
                            fout = new FileOutputStream(theAct.get().getExternalFilesDir(null).toString()+"/"+ze.getName());
                        }
                        BufferedOutputStream bout = new BufferedOutputStream(fout);
                        byte b[] = new byte[1024];
                        int n;

                        while ((n = zin.read(b,0,1024)) >= 0) {
                            bout.write(b,0,n);
                        }
                        /*
                        for (int c = zin.read(); c != -1; c = zin.read()) {
                            fout.write(c);
                        }
                        */
                        zin.closeEntry();
                        bout.close();
                    }

                }
                zin.close();
            } catch (Exception e) {
                Log.e("Decompress", "unzip", e);
            }

        }

        private void _dirChecker(String dir) {
            File f = new File(dir);

            if(!f.isDirectory()) {
                f.mkdirs();
            }
        }

        /*
        PROGRAMMER'S NOTE 3/23/2016: May consolidate the order "parsing" (ie, matching orders against
        my relevant criteria) into this method since building a List may be superfluous... after all,
        I don't care at all about orders that don't make the cut, whatever criteria dictate said cut
        */
        private List read(String fString){
            List resultList = new ArrayList();

            try {
                CSVReader reader = new CSVReader(new FileReader(fString));
                resultList = reader.readAll();
            } catch(IOException exception) {
                return resultList;
            }

            return resultList;

            /*
            BufferedReader reader = new BufferedReader(new InputStreamReader(fStream));
            try {
                String csvLine;
                while ((csvLine = reader.readLine()) != null) {
                    String[] row = csvLine.split(",");
                    //if (resultList.size() == 0)
                    Log.v(LOG_TAG,row.toString());
                    resultList.add(row);
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Error in reading CSV file: "+ex);
            }
            finally {
                try {
                    fStream.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Error while closing input stream: "+e);
                }
            }
            return resultList;
            */
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);

        }

    }

}
