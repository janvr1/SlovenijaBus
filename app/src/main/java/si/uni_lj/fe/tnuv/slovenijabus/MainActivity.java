package si.uni_lj.fe.tnuv.slovenijabus;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Calendar calendar;
    private int year, month, day;
    private TextView dateView;
    private EditText entryView;
    private EditText exitView;

    public static final String EXTRA_DATE = "intentData.DATE";
    public static final String EXTRA_ENTRY = "intentData.ENTRY";
    public static final String EXTRA_EXIT = "intentData.EXIT";

    public static final String API_voznired =
            "https://www.ap-ljubljana.si/_vozni_red/get_vozni_red_0.php"; // POST request
    public static final String API_postaje =
            "https://www.ap-ljubljana.si/_vozni_red/get_postajalisca_vsa_v2.php"; // GET request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);

        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);

        dateView = findViewById(R.id.datum_vnos);
        dateView.setText(dateStringBuilder(year, month, day));

        entryView = findViewById(R.id.vstopna_vnos);
        exitView = findViewById(R.id.izstopna_vnos);
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
        intent.putExtra(EXTRA_ENTRY, entryStation);
        intent.putExtra(EXTRA_EXIT, exitStation);
        intent.putExtra(EXTRA_DATE, date);
        startActivity(intent);
    }
}