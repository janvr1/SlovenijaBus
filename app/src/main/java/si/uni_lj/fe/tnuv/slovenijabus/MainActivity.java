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

    public static final String API_postaje =
            "https://www.ap-ljubljana.si/_vozni_red/get_postajalisca_vsa_v2.php"; // GET request

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php";

    public static final String EXTRA_DATE = "intentData.DATE";
    public static final String EXTRA_ENTRY = "intentData.ENTRY";
    public static final String EXTRA_EXIT = "intentData.EXIT";
    final ArrayList<HashMap<String, String>> favorites = new ArrayList<>();
    public ArrayList<String> station_names = new ArrayList<>();
    ArrayAdapter<String> adapter;
    DatabaseHelper slovenijabus_DB;
    private int year, month, day, end_day;
    private TextView dateView;
    private AutoCompleteTextView entryView;
    private AutoCompleteTextView exitView;
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

        favorites_rv.getItemAnimator().setChangeDuration(350);
        favorites_rv.getItemAnimator().setMoveDuration(350);
        favorites_rv.getItemAnimator().setAddDuration(350);
        favorites_rv.getItemAnimator().setRemoveDuration(350);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); // hint: Android šteje mesece od 0
        day = calendar.get(Calendar.DAY_OF_MONTH);
        updateFavorites();
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

    public void updateFavorites() {
        ArrayList<HashMap<String, String>> new_favorites = slovenijabus_DB.readFavorites();
        if (favoritesChanged(new_favorites, favorites)) {
            favorites.clear();
            favorites.addAll(slovenijabus_DB.readFavorites());
            favorites_recycler_adapter.notifyDataSetChanged();
        }

        for (int i = 0; i < favorites.size(); i++) {
            String request_data = createRequestString(slovenijabus_DB.getStationIDFromName(favorites.get(i).get("entry")),
                    slovenijabus_DB.getStationIDFromName(favorites.get(i).get("exit")),
                    dateStringBuilder(year, month, day));
            getNext3Buses(i, request_data);
        }
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

    public String createRequestString(String vstopID, String izstopID, String datum) {
        String request = "VSTOP_ID=" + vstopID + "&IZSTOP_ID=" + izstopID + "&DATUM=" + datum;
        return request;
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
        String[] splitted = input.split("\n");
        Map<String, String> station_map = new HashMap<>();

        for (int i = 1; i < splitted.length; i++) {
            String current = splitted[i];
            if (current.charAt(0) == "0".charAt(0)) {
                String x = current.substring(current.indexOf(":") + 1);
                String[] separated = x.split("\\|");
                station_map.put(separated[0], separated[1]);
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
                boolean change = false;

                if (next_buses.size() < (favorites.get(index).size() - 2)) {
                    change = true;
                    favorites.get(index).remove("first");
                    favorites.get(index).remove("second");
                    favorites.get(index).remove("third");
                }
                for (int i = 0; i < next_buses.size(); i++) {
                    if (i == 0) {
                        if (!next_buses.get(i).equals(favorites.get(index).get("first"))) {
                            change = true;
                        }
                        favorites.get(index).put("first", next_buses.get(i));
                    }
                    if (i == 1) {
                        if (!next_buses.get(i).equals(favorites.get(index).get("second"))) {
                            change = true;
                        }
                        favorites.get(index).put("second", next_buses.get(i));
                    }
                    if (i == 2) {
                        if (!next_buses.get(i).equals(favorites.get(index).get("third"))) {
                            change = true;
                        }
                        favorites.get(index).put("third", next_buses.get(i));
                    }
                }

                if (change) {
                    favorites_recycler_adapter.notifyItemChanged(index);
                }
            }
        }
    }

    public ArrayList<String> timetableParserFavorites(String input) {
        String[] splitted = input.split("\n");
        ArrayList<String> output = new ArrayList<>();

        String response_date_string = splitted[0].split("\\|")[6].substring(0, 10);

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date today_date = sdf2.parse(sdf2.format(new Date()));
            Date response_date = sdf2.parse(response_date_string);
            if (today_date.before(response_date)) {
                return output;
            }
        } catch (Exception e) {
            Log.d("main_act_parse_excptn", "No worky worky");
            Log.d("main_act_request_date", sdf2.format(new Date()));
            Log.d("main_act_response_date", response_date_string);
        }

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
        Log.d("timetableparser", output.toString());
        return output;
    }

    public void getNext3Buses(int i, String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_voznired);
        request.put("method", "POST");
        request.put("data", data);
        request.put("index", Integer.toString(i));
        Log.d("request_data", data);
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

    public boolean favoritesChanged(ArrayList<HashMap<String, String>> new_fav, ArrayList<HashMap<String, String>> old_fav) {
        if (new_fav.size() != old_fav.size()) {
            return true;
        }
        for (int i = 0; i < new_fav.size(); i++) {
            HashMap<String, String> hm_new, hm_old;
            hm_new = new_fav.get(i);
            hm_old = new_fav.get(i);
            if (!hm_new.get("entry").equals(hm_old.get("entry")) || !hm_new.get("exit").equals(hm_old.get("exit"))) {
                return true;
            }
        }
        return false;
    }
}