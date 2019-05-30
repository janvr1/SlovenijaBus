package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")

public class MainActivity extends AppCompatActivity implements DownloadCallback {

    private int year, month, day, end_day;
    private TextView dateView;
    private AutoCompleteTextView entryView;
    private AutoCompleteTextView exitView;

    public static final String EXTRA_DATE = "intentData.DATE";
    public static final String EXTRA_ENTRY = "intentData.ENTRY";
    public static final String EXTRA_EXIT = "intentData.EXIT";

    public static final String API_postaje =
            "https://www.ap-ljubljana.si/_vozni_red/get_postajalisca_vsa_v2.php"; // GET request

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php";

    //public static Map<String, String> stations_map = new HashMap<>();
    public ArrayList<String> station_names = new ArrayList<>();
    SimpleAdapter favorites_adapter;
    ArrayAdapter<String> adapter;
    final ArrayList<HashMap<String, String>> favorites = new ArrayList<>();

    DatabaseHelper slovenijabus_DB;

    private RecyclerView.Adapter favorites_recycler_adapter;
    private RecyclerView.LayoutManager favorites_layout_manager;
    private RecyclerView favorites_rv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getStationsFromAPI();
        slovenijabus_DB = DatabaseHelper.getInstance(this);

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); // hint: Android šteje mesece od 0
        day = calendar.get(Calendar.DAY_OF_MONTH);
        end_day = calendar.get(Calendar.DAY_OF_MONTH) + 14; // Datum za do dva tedna v naprej <—— To ne bo dobr delal, če je do konca meseca manj kot dva tedna ;)

        dateView = findViewById(R.id.datum_vnos);
        if (savedInstanceState != null) {
            dateView.setText(savedInstanceState.getString("date"));
        } else {
            dateView.setText(dateStringBuilder(year, month, day));
        }
        entryView = findViewById(R.id.vstopna_vnos);
        exitView = findViewById(R.id.izstopna_vnos);

        station_names.addAll(slovenijabus_DB.readStationsNames());
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, station_names);
        entryView.setAdapter(adapter);
        exitView.setAdapter(adapter);

        favorites.addAll(slovenijabus_DB.readFavorites());

        favorites_recycler_adapter = new favoritesRecyclerAdapter(this, favorites, dateView);
        favorites_rv = findViewById(R.id.favorites_recyclerview);
        favorites_layout_manager = new LinearLayoutManager(this);
        favorites_rv.setLayoutManager(favorites_layout_manager);
        favorites_rv.setAdapter(favorites_recycler_adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        favorites.clear();
        favorites.addAll(slovenijabus_DB.readFavorites());
        favorites_recycler_adapter.notifyDataSetChanged();

        for (int i = 0; i < favorites.size(); i++) {
            String request_data = "VSTOP_ID=" + slovenijabus_DB.getStationIDFromName(favorites.get(i).get("entry"))
                    + "&IZSTOP_ID=" + slovenijabus_DB.getStationIDFromName(favorites.get(i).get("exit")) + "&DATUM="
                    + dateStringBuilder(year, month, day);
            getNext3Buses(i, request_data);
        }
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

    public void launchShowAll(View view) {
        String entryStation = entryView.getText().toString();
        String exitStation = exitView.getText().toString();
        String date = dateView.getText().toString();
        Intent intent = new Intent(this, showAllActivity.class);
        intent.putExtra(EXTRA_ENTRY, slovenijabus_DB.getStationIDFromName(entryStation));
        intent.putExtra(EXTRA_EXIT, slovenijabus_DB.getStationIDFromName(exitStation));
        intent.putExtra(EXTRA_DATE, date);
        startActivity(intent);
    }

    public void makeHttpRequest(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask(this).execute(request);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    public void getStationsFromAPI() {
        HashMap<String, String> req = new HashMap<>();
        req.put("url", API_postaje);
        req.put("method", "GET");
        makeHttpRequest(req);
    }

    public void swapStations(View view) {
        String entry = entryView.getText().toString();
        String exit = exitView.getText().toString();
        entryView.setText(exit);
        exitView.setText(entry);
    }

    public Map<String, String> stationParser(String input) {
        //ArrayList<String> station_names = new ArrayList<>();
        String[] splitted = input.split("\n");
        Map<String, String> station_map = new HashMap<>();

        for (int i = 1; i < splitted.length; i++) {
            String current = splitted[i];
            if (current.charAt(0) == "0".charAt(0)) {
                String x = current.substring(current.indexOf(":") + 1);
                String[] separated = x.split("\\|");
                //stations_map.put(separated[1], separated[0]);
                station_map.put(separated[0], separated[1]);
                //station_names.add(separated[1]);
            }
        }
        return station_map;
    }

    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");
        String result_string = (String) result.get("response");

        if (result.get("response").equals("error")) {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (request.get("url").equals(API_postaje)) {
            Map<String, String> stations = stationParser(result_string);
            slovenijabus_DB.updateStations(stations);
            station_names.clear();
            station_names.addAll(slovenijabus_DB.readStationsNames());
            adapter.notifyDataSetChanged();

            //Toast.makeText(this, "Download postaj usepešen :)", Toast.LENGTH_SHORT).show();
        }

        if (request.get("url").equals(API_voznired)) {

            if (result_string.equals("error")) {
                return;
            }

            if (result_string.length() < 2) {
                return;
            }

            if (request.containsKey("index")) {
                int index = Integer.parseInt(request.get("index"));
                Log.d("favorites_index", request.get("index"));
                ArrayList<String> next_buses = timetableParserFavorites(result_string);
                for (int i = 0; i < next_buses.size(); i++) {
                    if (i == 0) {
                        favorites.get(index).put("first", next_buses.get(i));
                    }
                    if (i == 1) {
                        favorites.get(index).put("second", next_buses.get(i));
                    }
                    if (i == 2) {
                        favorites.get(index).put("third", next_buses.get(i));
                    }
                }
                favorites_recycler_adapter.notifyItemChanged(index);
                Log.d("favorites_array", "start");
                for (HashMap<String, String> hm : favorites) {
                    Log.d("favorites_array", hm.toString());
                }
                Log.d("favorites_array", "end");
            }
        }
    }

    public ArrayList<String> timetableParserFavorites(String input) {
        String[] splitted = input.split("\n");
        ArrayList<String> output = new ArrayList<>();
        for (int i = 0, j = 0; i < splitted.length && j < 3; i++) {
            String s = splitted[i];
            String[] separated = s.split("\\|");
            String time_str = separated[6].substring(0, separated[6].length() - 3);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = sdf.format(new Date());
            if (now.compareTo(time_str) < 1) {
                output.add(separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
                j++;
            }
        }
        return output;
    }

    public void getNext3Buses(int i, String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_voznired);
        request.put("method", "POST");
        request.put("data", data);
        request.put("index", Integer.toString(i));
        makeHttpRequest(request);
    }


    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
    }

    @Override
    public void finishDownloading() {
    }
}