package si.uni_lj.fe.tnuv.slovenijabus;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DownloadCallback {

    private Calendar calendar;
    private int year, month, day, end_day;
    private TextView dateView;
    private AutoCompleteTextView entryView;
    private AutoCompleteTextView exitView;

    public static final String EXTRA_DATE = "intentData.DATE";
    public static final String EXTRA_ENTRY = "intentData.ENTRY";
    public static final String EXTRA_EXIT = "intentData.EXIT";

    public static final String API_postaje =
            "https://www.ap-ljubljana.si/_vozni_red/get_postajalisca_vsa_v2.php"; // GET request

    public ArrayList<String> station_names = new ArrayList<String>();
    public Map<String, String> stations_map = new HashMap<String, String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        end_day = calendar.get(Calendar.DAY_OF_MONTH +14); // Datum za do dva tedna v naprej

        dateView = findViewById(R.id.datum_vnos);
        dateView.setText(dateStringBuilder(year, month, day));

        entryView = findViewById(R.id.vstopna_vnos);
        exitView = findViewById(R.id.izstopna_vnos);

        getStationsFromAPI();
    }

    public void setDate(View view) {
        showDialog(999);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 999) {
            return new DatePickerDialog(this, myDateListener, year, month, day);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog
            .OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int year, int month, int day) {
            dateView.setText(dateStringBuilder(year, month, day));

        }
    };

    private String dateStringBuilder(int year, int month, int day) {
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

    @Override
    public void updateFromDownload(Object result) {
        String stations_string = (String) result;
        String[] splitted = stations_string.split("\n");

        for (int i = 1; i < splitted.length - 1; i++) {
            String x = splitted[i].substring(splitted[i].indexOf(":") + 1);
            String[] separated = x.split("\\|");
            stations_map.put(separated[1], separated[0]);
            station_names.add(separated[1]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, station_names);
        entryView.setAdapter(adapter);
        exitView.setAdapter(adapter);

        Toast.makeText(this, "Download postaj usepešen :)", Toast.LENGTH_SHORT).show();
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

    public void getStationsFromAPI() {
        NetworkInfo netInfo = getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            new DownloadAsyncTask(this).execute(API_postaje);
        } else {
            Toast.makeText(this, R.string.network_error_message, Toast.LENGTH_SHORT).show();
        }
    }
}