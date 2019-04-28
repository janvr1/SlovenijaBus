package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class showAllActivity extends AppCompatActivity implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request
    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    private String request_data;

    /*Test*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        request_data = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date;
        getTimetablesFromAPI();

        String entryName = MainActivity.stations_map.get(entryStationID);
        String exitName = MainActivity.stations_map.get(exitStationID);

        TextView title = findViewById(R.id.show_all_title);
        title.setText("Vozni red: " + entryName + " do " + exitName);
        Toast.makeText(this, "Request string: " + request_data, Toast.LENGTH_LONG).show();

    }

    public void getTimetablesFromAPI() {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask_POST(this).execute(API_voznired, request_data);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<HashMap<String, String>> timetableParser(String input) {
        String[] splitted = input.split("\n");
        ArrayList<HashMap<String, String>> output = new ArrayList<>();

        for (int i = 0; i < splitted.length; i++) {
            String[] separated = splitted[i].split("\\|");
            HashMap<String, String> timetable = new HashMap<>();
            timetable.put("entry_time", separated[6]);
            timetable.put("exit_time", separated[7]);
            timetable.put("duration", separated[8]);
            output.add(timetable);
        }
        return output;
    }


    @Override
    public void updateFromDownload(Object result) {
        String voznired_response = (String) result;
        //Toast.makeText(this, voznired_response, Toast.LENGTH_SHORT).show();
        ArrayList<HashMap<String, String>> timetable = timetableParser(voznired_response);

        ListView lv = findViewById(R.id.show_all_list);
        ListAdapter adapter = new SimpleAdapter(this, timetable, R.layout.show_all_list_item,
                new String[]{"entry_time", "exit_time", "duration"},
                new int[]{R.id.entry_time, R.id.exit_time, R.id.duration});
        lv.setAdapter(adapter);
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
