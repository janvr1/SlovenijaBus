package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class showSingleActivity extends AppCompatActivity implements DownloadCallback {

    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_single);

        Intent intent = getIntent();
        String line_data = intent.getStringExtra(showAllActivity.EXTRA_LINE_DATA);
        getLineDataFromAPI(line_data);
    }

    public void getLineDataFromAPI(String data) {
        HashMap<String, String> request = new HashMap<>();
        request.put("url", API_podatki_relacija);
        request.put("method", "POST");
        request.put("data", "flags=" + data);
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

    public HashMap<String, Object> lineDataParser(String input) {
        String[] splitted = input.split("\n");
        HashMap<String, Object> output = new HashMap<>();
        String start = splitted[0].split("\\|")[1];
        output.put("start", start);
        String destination = splitted[splitted.length - 1].split("\\|")[1];
        output.put("end", destination);
        String company = splitted[0].split("\\|")[0];
        output.put("company", company);
        ArrayList<String[]> visitedStations = new ArrayList<>();

        for (int i = 1; i < splitted.length - 1; i++) {
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
        output.put("visited_stations", visitedStations);
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

        if (request.get("url").equals(API_podatki_relacija)) {
            String line_data_str = (String) result.get("response");
            HashMap<String, Object> line_data = lineDataParser(line_data_str);
            ArrayList<String> line_data_array = new ArrayList<>();
            line_data_array.add("Prevoznik: " + line_data.get("company"));
            line_data_array.add("Relacija: " + line_data.get("start") + " - " + line_data.get("end"));
            line_data_array.add("Postaje:");
            for (String[] s : (ArrayList<String[]>) line_data.get("visited_stations")) {
                String station = s[0] + " " + s[1];
                if (s[2] != "") {
                    station += " " + s[2];
                }
                line_data_array.add(station);
            }
            ListView lv = findViewById(R.id.show_single_list);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                    line_data_array);
            lv.setAdapter(adapter);
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

