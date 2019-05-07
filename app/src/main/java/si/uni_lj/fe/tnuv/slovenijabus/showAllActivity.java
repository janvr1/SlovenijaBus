package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class showAllActivity extends AppCompatActivity implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request

    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    public static final String EXTRA_LINE_DATA = "intentData.LINE_DATA";

    List<List<HashMap<String, String>>> listOfChildGroups;
    SimpleExpandableListAdapter adapter;

    public ArrayList<Integer> alreadyDownloadedLines = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        String request_data = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date;
        getTimetablesFromAPI(request_data);

        String entryName = MainActivity.stations_map.get(entryStationID);
        String exitName = MainActivity.stations_map.get(exitStationID);

        TextView vstop = findViewById(R.id.show_all_vstop);
        vstop.setText(getString(R.string.showall_entry_station, entryName));
        TextView izstop = findViewById(R.id.show_all_izstop);
        izstop.setText(getString(R.string.showall_exit_station, exitName));
        TextView datum = findViewById(R.id.show_all_datum);
        datum.setText(getString(R.string.showall_date, date));
        Toast.makeText(this, "Request string: " + request_data, Toast.LENGTH_LONG).show();

    }

    public void getTimetablesFromAPI(String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_voznired);
        request.put("method", "POST");
        request.put("data", data);

        makeHttpRequest(request);
    }

    public void makeHttpRequest(HashMap<String, String> request) {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask(this).execute(request);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<HashMap<String, String>> timetableParser(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> output = new ArrayList<>();

        for (String s : splitted) {
            String[] separated = s.split("\\|");
            HashMap<String, String> timetable = new HashMap<>();
            timetable.put("entry_time", separated[6].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("exit_time", separated[7].substring(11, 16).replaceFirst("^0+(?!$)", ""));
            timetable.put("entry_time_long", separated[6]);
            timetable.put("exit_time_long", separated[7]);
            timetable.put("duration", separated[8]);
            timetable.put("price", separated[9]);
            timetable.put("line_data", separated[13]);
            output.add(timetable);
        }
        return output;
    }


    @Override
    public void updateFromDownload(Object res) {
        HashMap<String, Object> result = (HashMap<String, Object>) res;
        HashMap<String, String> request = (HashMap<String, String>) result.get("request");

        if (result.get("response").equals("error")) {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (request.get("url").equals(API_voznired)) {
            String voznired_response = (String) result.get("response");
            final ArrayList<HashMap<String, String>> timetable = timetableParser(voznired_response);

            listOfChildGroups = new ArrayList<List<HashMap<String, String>>>();
            for (int i = 0; i < timetable.size(); i++) {
                listOfChildGroups.add(i, new ArrayList<HashMap<String, String>>());
            }

            ExpandableListView lv = findViewById(R.id.show_all_list);

            String[] parentFromArray = {"entry_time", "exit_time", "duration"};
            int[] parentToArray = {R.id.entry_time, R.id.exit_time, R.id.duration};

//            String[] childFromArray = {"relacija", "prevoznik", "postaja"};
//            int[] childToArray = {R.id.dropdown_relacija, R.id.dropdown_prevoznik, R.id.dropdown_postaja};
            String[] childFromArray = {"label", "data"};
            int[] childToArray = {R.id.dropdown_item_label, R.id.dropdown_item_data};


            adapter = new SimpleExpandableListAdapter(this,
                    timetable, R.layout.show_all_list_item, parentFromArray, parentToArray,
                    listOfChildGroups, R.layout.dropdown_list_item, childFromArray, childToArray);
            lv.setAdapter(adapter);
            lv.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int groupPosition) {
                    if (alreadyDownloadedLines.contains(groupPosition)) {
                        return;
                    }
                    HashMap<String, String> group = (HashMap<String, String>) adapter.getGroup(groupPosition);
                    String req_data = group.get("line_data");
                    getLineDataFromAPI(req_data, groupPosition);
                    alreadyDownloadedLines.add(groupPosition);
                    //Toast.makeText(showAllActivity.this, req_data, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (request.get("url").equals(API_podatki_relacija)) {
            String line_data_str = (String) result.get("response");
            HashMap<String, Object> line_data = lineDataParser(line_data_str);

            int groupPosition = Integer.parseInt(request.get("group"));
            List childGroup = listOfChildGroups.get(groupPosition);

            HashMap<String, String> prevoznikmap = new HashMap<>();
            prevoznikmap.put("label", getString(R.string.prevoznik) + ":");
            prevoznikmap.put("data", (String) line_data.get("company"));
            childGroup.add(prevoznikmap);

            HashMap<String, String> relacijamap = new HashMap<>();
            relacijamap.put("label", getString(R.string.relacija) + ":");
            relacijamap.put("data", (String) line_data.get("start_end"));
            childGroup.add(relacijamap);

            for (HashMap<String, String> s :
                    (ArrayList<HashMap<String, String>>) line_data.get("visited_stations")) {
                HashMap<String, String> postaja = new HashMap<>();
                postaja.put("label", s.get("time"));
                if (s.containsKey("wait")) {
                    postaja.put("data", s.get("station") + "(" + s.get("wait") + ")");
                } else {
                    postaja.put("data", s.get("station"));
                }
                childGroup.add(postaja);
            }


/*            String prevoznik = (String) getString(R.string.prevoznik) + " " + line_data.get("company");
            String relacija = (String) getString(R.string.relacija) + " "
                    + line_data.get("start") + " - " + line_data.get("end");*/
/*
            HashMap<String, String> prevoznikrelacija = new HashMap<>();

            prevoznikrelacija.put("prevoznik", prevoznik);
            prevoznikrelacija.put("relacija", relacija);
            group.add(prevoznikrelacija);
*/
/*            HashMap<String, String> prevoznikmap = new HashMap<>();
            prevoznikmap.put("dropdown_data", prevoznik);
            HashMap<String, String> relacijamap = new HashMap<>();
            relacijamap.put("dropdown_data", relacija);
            group.add(prevoznikmap);
            group.add(relacijamap);

            for (String[] s : (ArrayList<String[]>) line_data.get("visited_stations")) {
                String station = s[0] + " " + s[1];
                if (s[2] != "") {
                    station += " " + s[2];
                }
                HashMap<String, String> postaja = new HashMap<>();
                postaja.put("dropdown_data", station);
                group.add(postaja);
            }*/

            adapter.notifyDataSetChanged();
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

    public void launchShowSingle(View view) {
/*        LinearLayout listItem = (LinearLayout) view;
        TextView line_data = (TextView) listItem.getChildAt(0);
        String line_data_str = line_data.getText().toString();
        Intent intent = new Intent(this, showSingleActivity.class);
        intent.putExtra(EXTRA_LINE_DATA, line_data_str);
        startActivity(intent);*/

    }

    public HashMap<String, Object> lineDataParser(String input) {
        String[] splitted = input.split("\n");
        HashMap<String, Object> output = new HashMap<>();

        String start = splitted[0].split("\\|")[1];
        output.put("start", start);

        String destination = splitted[splitted.length - 1].split("\\|")[1];
        output.put("end", destination);

        String startEnd = start + " - " + destination;
        output.put("start_end", startEnd);

        String company = splitted[0].split("\\|")[0];
        output.put("company", company);

        ArrayList<HashMap<String, String>> visitedStations = new ArrayList<>();

/*        for (int i = 1; i < splitted.length - 1; i++) {
            if (i == 1) {
                String[] s = splitted[i].split("\\|");
                s = Arrays.copyOfRange(s, 1, s.length);
                s[1] = s[1].substring(11, 16);
                String[] ss = {s[1], s[0], ""};
                if (s.length > 3) {
                    ss[2] = s[3];
                }
                visitedStations.add(ss);
                continue;
            }
            String[] s = splitted[i].split("\\|");
            s[2] = s[2].substring(11, 16);
            String[] ss = {s[2], s[1], ""};
            if (s.length > 4) {
                ss[2] = s[4];
            }
            visitedStations.add(ss);
        }
        output.put("visited_stations", visitedStations);*/
        for (int i = 1; i < splitted.length - 1; i++) {
            if (i == 1) {
                String[] s = splitted[i].split("\\|");
                s = Arrays.copyOfRange(s, 1, s.length);
                s[1] = s[1].substring(11, 16);
                HashMap<String, String> ss = new HashMap<>();
                ss.put("time", s[1]);
                ss.put("station", s[0]);
                if (s.length > 3) {
                    ss.put("wait", s[3]);
                }
                visitedStations.add(ss);
                continue;
            }
            String[] s = splitted[i].split("\\|");
            s[2] = s[2].substring(11, 16);
            HashMap<String, String> ss = new HashMap<>();
            ss.put("time", s[2]);
            ss.put("station", s[1]);
            if (s.length > 4) {
                ss.put("wait", s[4]);
            }
            visitedStations.add(ss);
        }
        output.put("visited_stations", visitedStations);
        return output;
    }

    public void getLineDataFromAPI(String data, int i) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
        request.put("group", Integer.toString(i));
        makeHttpRequest(request);
    }

    public void addToFavorites(View view) {
        Set<String> favorites = readFavorites();
        Set<String> newFavorites = new HashSet<>();
        newFavorites.addAll(favorites);

        Intent intent = getIntent();
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);
        String toWrite = entryStationID + ";" + exitStationID;

        newFavorites.add(toWrite);
        writeFavorites(newFavorites);

        Toast.makeText(this, toWrite, Toast.LENGTH_SHORT).show();


    }


    public Set<String> readFavorites() {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preferences_key), Context.MODE_PRIVATE);
        return sharedPref.getStringSet("favorites", new HashSet<String>());
    }

    public void writeFavorites(Set<String> fav) {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preferences_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("favorites", fav);
        editor.commit();
    }
}
