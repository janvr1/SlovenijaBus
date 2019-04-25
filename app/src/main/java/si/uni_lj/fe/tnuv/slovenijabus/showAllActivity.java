package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class showAllActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStation = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStation = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        String merged = entryStation + " " + exitStation + " " + date;
        Toast.makeText(this, merged, Toast.LENGTH_LONG).show();
    }
}
