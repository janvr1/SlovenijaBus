package si.uni_lj.fe.tnuv.slovenijabus;

//import android.os.CountDownTimer;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;


// Progress bar
import android.widget.ProgressBar;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

// Date
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class etaActivity extends AppCompatActivity {

    /*
    private ProgressBar progressBar;

    // How to convert string to date
    // simple date format (dolocis obliko zaisa datuma)
    // poparsas string
    // get difference -ish
    private int cas_odhoda; // poloadaj iz API-ja; intent data array hash map-ov;
    private int cas_prihoda;
    private int trenuten_cas;
    private long trenuten_cas_l;
    private int totalETA = cas_prihoda - cas_odhoda; // Predviden cas potovanja (V SEKUNDAH?)
    private int elapsedTime = 0; // Zapolnjen progress bar ob inicializaciji (pretecen cas od odhoda avtobusa)

    private Handler handler = new Handler();
    private Calendar cal = Calendar.getInstance();
    */

    // postaje
    DatabaseHelper slovenijabus_DB;
    boolean invalid_station;
    private AutoCompleteTextView entryView;
    private AutoCompleteTextView exitView;

    // datum
    private TextView dateView;
    private int year, month, day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eta);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));
        }


        slovenijabus_DB = DatabaseHelper.getInstance(this);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        invalid_station = entryStationID == null || exitStationID == null;

        String request_data = createRequestString(entryStationID, exitStationID, date);

        // Izpis in nastavitev datuma v textView
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); // hint: Android šteje mesece od 0
        day = calendar.get(Calendar.DAY_OF_MONTH);
        //end_day = calendar.get(Calendar.DAY_OF_MONTH) + 14; // Datum za do dva tedna v naprej <—— To ne bo dobr delal, če je do konca meseca manj kot dva tedna ;)
        dateView = findViewById(R.id.datum_vnos_eta);

        if (savedInstanceState != null) {
            dateView.setText(savedInstanceState.getString("date"));
        } else {
            dateView.setText(dateStringBuilder(year, month, day));
        }


        // Oblikovanje zapisa datuma
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date dateToday = sdf.parse(date); // date1 preimenovan -> dateToday
            Calendar c = Calendar.getInstance();
            c.setTime(dateToday);
        } catch (Exception e) {
//            Log.d("parse exception", "something no worky worky");
        }

        // Pridobi ID/imena vstopne in izstopne postaje ter ju izpisi na zaslon
        String entryName = slovenijabus_DB.getStationNameFromID(entryStationID);
        String exitName = slovenijabus_DB.getStationNameFromID(exitStationID);

        TextView vstop = findViewById(R.id.krajOdhod);
        vstop.setText(entryName);
        TextView izstop = findViewById(R.id.krajPrihod);
        izstop.setText(exitName);

        // Pridobi podatke iz parserja in ustvari seznam
        List<HashMap<String, Object>> timetableList = new ArrayList<>();
        Object tbObject = timetableList.add(timetableFragment.timetableParser(request_data)); //.get("timetable")
        tbObject.get("timetable");

        HashMap<String, String> busData;
        busData = timetableList.get(0);

        String departure, arrival, durationEstimate;
        departure = busData.get("entry_time");
        arrival = busData.get("exit_time");
        durationEstimate = busData.get("duration");
        Log.d("odhod", departure);
        Log.d("prihod", arrival);
        Log.d("eta", durationEstimate);

//        for (HashMap<String, String> map : fragmentList){
//            for (Map.Entry<String, String> mapEntry : map.entrySet()) {
//                String key = mapEntry.getKey();
//                Log.d("KEY", key);
//                String value = mapEntry.getValue();
//                Log.d("VALUE", value);
//            }
//        }
//        Log.d("LOAD", fragmentList.get(0).get);


        // Menjaj smer
        final ImageButton swap_direction = findViewById(R.id.zamenjajSmer);
        swap_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDirection(v);
            }
        });

        // ##### STARA KODA - RECIKLIRAJ PO DELIH #####
        /*
        trenuten_cas_l = cal.getTime();
        trenuten_cas = (int)(trenuten_cas_l % Integer.MAX_VALUE);

        progressBar=(ProgressBar) findViewById(R.id.potETA);
        totalETA = 1440; // totalETA = cas_prihoda - cas_odhoda --> pretvori v sekunde (1440s == 24min ); podatke o casih dobimo prek API-ja

        if (/* trenuten_cas - cas_odhoda <= 0  1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (elapsedTime < totalETA) {

                        // Pridobi nov elapsedTime

                        handler.post(new Runnable() {
                            public void run() {
                                progressBar.setProgress(elapsedTime);
                                //textView.setText(progressStatus + "/" + progressBar.getMax());
                            }
                        });
                        try {
                            // Sleep for 3 seconds.
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        */

    }



    @Override
    protected void onStart() {
        super.onStart();
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); // hint: Android šteje mesece od 0
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("date", dateView.getText().toString());
    }

    public void setDate(View view) {
        DialogFragment df = new SetDateFragment();
        df.show(getSupportFragmentManager(), "date_entry");
    }

    private String dateStringBuilder(int year, int month, int day) {
        month++; // da je pravilen mesec :)
        return day + "." + month + "." + year;
    }


    public String createRequestString(String vstopID, String izstopID, String datum) {
        String request = "VSTOP_ID=" + vstopID + "&IZSTOP_ID=" + izstopID + "&DATUM=" + datum;
        return request;
    }

    public void changeDirection(View view) {
        if (invalid_station) {
            Toast.makeText(this, getString(R.string.invalid_station_name), Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        Intent newIntent = new Intent(this, etaActivity.class);
        newIntent.putExtra(MainActivity.EXTRA_ENTRY, exitStationID);
        newIntent.putExtra(MainActivity.EXTRA_EXIT, entryStationID);
        newIntent.putExtra(MainActivity.EXTRA_DATE, date);
        startActivity(newIntent);
        finish();
    }

    public void launchShowAll(View view) {
        String entryStation = entryView.getText().toString();
        String exitStation = exitView.getText().toString();
        String date = dateView.getText().toString();
        Intent intent = new Intent(this, showAllActivity.class);
        intent.putExtra(MainActivity.EXTRA_ENTRY, slovenijabus_DB.getStationIDFromName(entryStation));
        intent.putExtra(MainActivity.EXTRA_EXIT, slovenijabus_DB.getStationIDFromName(exitStation));
        intent.putExtra(MainActivity.EXTRA_DATE, date);
        startActivity(intent);
    }



    //CountDownTimer mCountDownTimer;



    /*mProgressBar.setProgress(i);
        mCountDownTimer=new CountDownTimer(5000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.v("Log_tag", "Tick of Progress"+ i+ millisUntilFinished);
                i++;
                mProgressBar.setProgress((int)i*100/(5000/1000));

            }

            @Override
            public void onFinish() {
                //Do what you want
                i++;
                mProgressBar.setProgress(100);
            }
        };
        mCountDownTimer.start();*/

}
