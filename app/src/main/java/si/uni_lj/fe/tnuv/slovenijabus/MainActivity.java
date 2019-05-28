package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    public static Map<String, String> stations_map = new HashMap<>();

    SimpleAdapter favorites_adapter;
    final ArrayList<HashMap<String, String>> favorites = new ArrayList<>();

    DatabaseHelper favorites_db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getStationsFromAPI();
        favorites_db = DatabaseHelper.getInstance(this);

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH); // hint: Android šteje mesece od 0
        day = calendar.get(Calendar.DAY_OF_MONTH);
        end_day = calendar.get(Calendar.DAY_OF_MONTH) + 14; // Datum za do dva tedna v naprej <—— To ne bo dobr delal, če je do konca meseca manj kot dva tedna ;)

        dateView = findViewById(R.id.datum_vnos);
        dateView.setText(dateStringBuilder(year, month, day));

        entryView = findViewById(R.id.vstopna_vnos);
        exitView = findViewById(R.id.izstopna_vnos);

        ListView fav_lv = findViewById(R.id.favorites_listview);

        favorites.addAll(favorites_db.readFavorites());
        String[] fromArray = {"entry", "exit"};
        int[] toArray = {R.id.favorites_from, R.id.favorites_to};

        favorites_adapter = new SimpleAdapter(this, favorites, R.layout.favorites_list_item,
                fromArray, toArray);

        fav_lv.setAdapter(favorites_adapter);

        fav_lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                TextView from = view.findViewById(R.id.favorites_from);
                String entry = from.getText().toString();
                TextView to = view.findViewById(R.id.favorites_to);
                String exit = to.getText().toString();
                favorites.remove(position);
                favorites_adapter.notifyDataSetChanged();
                favorites_db.removeFavorite(entry, exit);
                dumpDBtoLog();
                Toast.makeText(MainActivity.this, R.string.remove_from_favorites, Toast.LENGTH_LONG).show();
                return true;
            }
        });

        fav_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> clicked = favorites.get(position);
                String entryID = stations_map.get(clicked.get("entry"));
                String exitID = stations_map.get(clicked.get("exit"));

                Intent intent = new Intent(getApplicationContext(), showAllActivity.class);

                intent.putExtra(EXTRA_ENTRY, entryID);
                intent.putExtra(EXTRA_EXIT, exitID);
                intent.putExtra(EXTRA_DATE, dateView.getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        favorites.clear();
        favorites.addAll(favorites_db.readFavorites());
        favorites_adapter.notifyDataSetChanged();
        dumpDBtoLog();
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
        intent.putExtra(EXTRA_ENTRY, stations_map.get(entryStation));
        intent.putExtra(EXTRA_EXIT, stations_map.get(exitStation));
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

    public ArrayList<String> stationParser(String input) {
        ArrayList<String> station_names = new ArrayList<>();
        String[] splitted = input.split("\n");

        for (int i = 1; i < splitted.length; i++) {
            String current = splitted[i];
            if (current.charAt(0) == "0".charAt(0)) {
                String x = current.substring(current.indexOf(":") + 1);
                String[] separated = x.split("\\|");
                stations_map.put(separated[1], separated[0]);
                stations_map.put(separated[0], separated[1]);
                station_names.add(separated[1]);
            }
        }
        return station_names;
    }

    public ArrayList<HashMap<String, String>> readFavorites() {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preferences_key), Context.MODE_PRIVATE);

        Set<String> fav = sharedPref.getStringSet("favorites", new LinkedHashSet<String>());
        ArrayList<HashMap<String, String>> fav_array = new ArrayList<>();
        for (String s : fav) {
            String[] ss = s.split(";");
            HashMap<String, String> map = new HashMap<>();
            map.put("from", ss[0]);
            map.put("to", ss[1]);
            fav_array.add(map);
        }
        return fav_array;

    }

    public void writeFavorites(ArrayList<HashMap<String, String>> fav) {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preferences_key), Context.MODE_PRIVATE);
        Set<String> favorites = new LinkedHashSet<>();
        //ArrayList<String> fav2 = new ArrayList<>();
        for (HashMap<String, String> map : fav) {
            favorites.add(TextUtils.join(";", new String[]{map.get("from"), map.get("to")}));
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("favorites", favorites);
        editor.apply();
    }

    public void dumpDBtoLog() {
        for (HashMap<String, String> hm : favorites_db.readFavorites()) {
            Log.d("db", hm.toString());
        }
    }

    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");

        if (result.get("response").equals("error")) {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (request.get("url").equals(API_postaje)) {
            String stations_string = (String) result.get("response");
            ArrayList<String> station_names = stationParser(stations_string);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, station_names);
            entryView.setAdapter(adapter);
            exitView.setAdapter(adapter);

            Toast.makeText(this, "Download postaj usepešen :)", Toast.LENGTH_SHORT).show();
        }
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