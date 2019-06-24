package si.uni_lj.fe.tnuv.slovenijabus;

//import android.os.CountDownTimer;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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

//import static si.uni_lj.fe.tnuv.slovenijabus.timetableFragment.timetableParser;

public class etaActivity extends AppCompatActivity { // implements DownloadCallback
    /**
     * ŠE NEIMPLEMENTIRAN ACTIVITY (tretja iteracija)
     * Dostop iz aplikacije ni možen
     */

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

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request

    // postaje
    public ArrayList<String> station_names = new ArrayList<>();
    ArrayAdapter<String> adapter;
    DatabaseHelper slovenijabus_DB;

    // API
    TextView msg;
    private static final String ARG_REQUEST_STRING = "req_str";
    private static final String ARG_INVALID_STATION = "invalid_station";
    public String request_string;
    boolean invalid_station;
    private TextView entryView;
    private TextView exitView;

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

        station_names.addAll(slovenijabus_DB.readStationsNames());
        entryView = findViewById(R.id.krajOdhod);
        exitView = findViewById(R.id.krajPrihod);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, station_names);

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

        //Log.d("reqData", request_data);


//        List<HashMap<String, String>> timetableList = new ArrayList<>();
//        timetableList.addAll((ArrayList<HashMap<String, String>>) timetableParser(request_data).get("timetable")); //.get("timetable") Object tbObject = tbObject.get("timetable");
//
//
//        HashMap<String, String> busData;
//        busData = timetableList.get(0);
//
//        String departure, arrival, durationEstimate;
//        departure = busData.get("entry_time");
//        arrival = busData.get("exit_time");
//        durationEstimate = busData.get("duration");
//        Log.d("odhod", departure);
//        Log.d("prihod", arrival);
//        Log.d("eta", durationEstimate);

//        for (HashMap<String, String> map : fragmentList){
//            for (Map.Entry<String, String> mapEntry : map.entrySet()) {
//                String key = mapEntry.getKey();
//                Log.d("KEY", key);
//                String value = mapEntry.getValue();
//                Log.d("VALUE", value);
//            }
//        }
//        Log.d("LOAD", fragmentList.get(0).get);


        // Priljubljene
        if (slovenijabus_DB.checkIfInFavorites(entryName, exitName)) {
            ImageButton fav_btn = findViewById(R.id.priljubljene);
            fav_btn.setImageResource(R.drawable.heart_full_white);
        }


        // Menjaj smer
        final ImageButton swap_direction = findViewById(R.id.zamenjajSmer);
        swap_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDirection(v);
            }
        });

        // ##### NEIMPLEMENTIRANA KODA - PROGRESSBAR #####
        // Poenostavi na navaden timer
        // predviden cas voznje (duration) vrne stevilo segmentov vrstice napredka
        // poglej razliko med trenutnim casom in casom odhoda - to odstej od zacetnega casa voznje
        // inicializiraj na primerni vrednosti in pozeni odstevalnik casa - z odstevanjem casa se polni vrstica napredka
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

    public void makeHttpRequest(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask().execute(request);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    public void getTimetablesFromAPI(String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_voznired);
        request.put("method", "POST");
        request.put("data", data);

        makeHttpRequest(request);
    }

    public static HashMap<String, Object> timetableParser(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> outputTimetable = new ArrayList<>();
        boolean first = true;
        int first_index = 0;
        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            String[] separated = s.split("\\|");
            String time_str = separated[6].substring(0, separated[6].length() - 3);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = sdf.format(new Date());

            if (now.compareTo(time_str) < 1 && first) {
                first = false;
                first_index = i;
            }
            HashMap<String, String> timetable = new HashMap<>();
            timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("date", separated[6].substring(0, 10));
            timetable.put("entry_time_long", separated[6]);
            timetable.put("exit_time_long", separated[7]);
            timetable.put("duration", separated[8]);
            timetable.put("price", separated[9].replace(".", ",") + " €");
            timetable.put("line_data", separated[13]);
            outputTimetable.add(timetable);
        }
        if (first) {
            first_index = outputTimetable.size();
        }

        HashMap<String, Object> output = new HashMap<>();
        output.put("timetable", outputTimetable);
        output.put("index", first_index);
        return output;
    }

    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");
        String result_string = (String) result.get("response");

        if (result.get("response").equals("error")) {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (request.get("url").equals(API_voznired)) {

            if (result_string.equals("error")) {
                return;
            }

            if (result_string.length() < 2) {
                Toast.makeText(this, R.string.no_buses_message, Toast.LENGTH_LONG).show();
                return;
            }

            List<HashMap<String, String>> timetableList = new ArrayList<>();
            timetableList.addAll((ArrayList<HashMap<String, String>>) timetableParser(result_string).get("timetable")); //.get("timetable") Object tbObject = tbObject.get("timetable");
            String departure, arrival, durationEstimate;


            HashMap<String, String> busData;
            for (int i = 0; i < timetableList.size(); i++)
            {
                busData = timetableList.get(i);

                departure = busData.get("entry_time");
                arrival = busData.get("exit_time");
                durationEstimate = busData.get("duration");
            }
            busData = timetableList.get(0);



            HashMap<String, Object> data = timetableParser(result_string);
            int first_index = (int) data.get("index");
            ArrayList<HashMap<String, String>> timetable = (ArrayList<HashMap<String, String>>) data.get("timetable");

            SimpleDateFormat sdf_request = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat sdf_response = new SimpleDateFormat("yyyy-MM-dd");



            /*
            try {
                Date request_date = sdf_request.parse(timetableFragment.request_string.split("=")[3]);
                Date response_date = sdf_response.parse(timetable.get(0).get("date"));
                if (request_date.before(response_date)) {
                    Toast.makeText(this, R.string.no_buses_on_this_day, Toast.LENGTH_LONG).show();
                    //msg.setText(getString(R.string.no_buses_on_this_day, sdf_request.format(response_date)));
                    //msg.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
/*                Log.d("fragment_parse_excptn", "No worky worky");
                Log.d("fragment_request_date", request_string.split("=")[3]);
                Log.d("fragment_response_date", timetable.get(0).get("date"));*
            }
            */
            //showAll_rv.setVisibility(View.VISIBLE);

            String[] parentFromArray = {"entry_time", "exit_time", "duration"};
            int[] parentToArray = {R.id.trenutenOdhod, R.id.trenutenPrihod, R.id.izpisETA};
        }
    }

    public void getNext3Buses(int i, String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", MainActivity.API_voznired);
        request.put("method", "POST");
        request.put("data", data);
        request.put("index", Integer.toString(i));
        Log.d("request_data", data);
        makeHttpRequest(request);
    }

    public void getStationsFromAPI() {
        HashMap<String, String> req = new HashMap<>();
        req.put("url", MainActivity.API_postaje);
        req.put("method", "GET");
        makeHttpRequest(req);
    }

    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
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

    public void favoritesButton(View view) {
        if (invalid_station) {
            Toast.makeText(this, getString(R.string.invalid_station_name), Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = getIntent();
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);
        String entryName = slovenijabus_DB.getStationNameFromID(entryStationID);
        String exitName = slovenijabus_DB.getStationNameFromID(exitStationID);
        boolean isIn = slovenijabus_DB.checkIfInFavorites(entryName, exitName);

        ImageButton fav_btn = findViewById(R.id.priljubljene);

        if (isIn) {
            slovenijabus_DB.removeFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_empty_white);
            Toast.makeText(this, getString(R.string.remove_from_favorites), Toast.LENGTH_LONG).show();
        } else {
            slovenijabus_DB.addFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_full_white);
            Toast.makeText(this, getString(R.string.add_to_favorites), Toast.LENGTH_LONG).show();
        }
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
