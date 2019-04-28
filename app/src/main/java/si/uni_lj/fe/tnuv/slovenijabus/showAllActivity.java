package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class showAllActivity extends AppCompatActivity implements DownloadCallback {

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request
    public static final String API_podatki_relacija =
            "https://www.ap-ljubljana.si/_vozni_red/get_linija_info_0.php"; // POST request

    private String request_data;
    private String voznired_response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStation = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStation = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        request_data = "VSTOP_ID=" + entryStation + "&IZSTOP_ID=" + exitStation + "&DATUM=" + date;
        getTimetablesFromAPI();
        //Toast.makeText(this, merged, Toast.LENGTH_LONG).show();

    }

    public void getTimetablesFromAPI() {
        NetworkInfo netInfo = getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask_POST(this).execute(API_voznired, request_data);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<String[]> timeTableParser(String input) {
        String[] splitted = input.split("\n");
        ArrayList<String[]> timetable = new ArrayList<String[]>();

        for (int i = 0; i < splitted.length - 1; i++) {
            String[] separated = splitted[i].split("\\|");
            timetable.add(separated);
        }
        return timetable;
    }


    @Override
    public void updateFromDownload(Object result) {
        voznired_response = (String) result;
        Toast.makeText(this, voznired_response, Toast.LENGTH_SHORT).show();
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
