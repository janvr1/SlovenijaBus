package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class showAllActivity extends AppCompatActivity {

    DatabaseHelper slovenijabus_DB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Toolbar myToolbar = findViewById(R.id.show_all_toolbar);
        setSupportActionBar(myToolbar);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));
        }
        slovenijabus_DB = DatabaseHelper.getInstance(this);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);
        boolean invalid_station;
        invalid_station = entryStationID == null || exitStationID == null;

        //String request_data = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date;
        String request_data = createRequestString(entryStationID, exitStationID, date);

        String date2 = null, date3 = null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date date1 = sdf.parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(date1);
            c.add(Calendar.DAY_OF_MONTH, 1);
            date2 = dateStringBuilder(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            c.add(Calendar.DAY_OF_MONTH, 1);
            date3 = dateStringBuilder(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        } catch (Exception e) {
//            Log.d("parse exception", "something no worky worky");
        }

        String request_data2 = createRequestString(entryStationID, exitStationID, date2);
        String request_data3 = createRequestString(entryStationID, exitStationID, date3);

        String entryName = slovenijabus_DB.getStationNameFromID(entryStationID);
        String exitName = slovenijabus_DB.getStationNameFromID(exitStationID);

        TextView vstop = findViewById(R.id.show_all_vstop);
        vstop.setText(entryName);
        TextView izstop = findViewById(R.id.show_all_izstop);
        izstop.setText(exitName);

        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.setOffscreenPageLimit(2);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(timetableFragment.newInstance(request_data, invalid_station));
        fragmentList.add(timetableFragment.newInstance(request_data2, invalid_station));
        fragmentList.add(timetableFragment.newInstance(request_data3, invalid_station));

        List<String> titleList = new ArrayList<>();
        titleList.add(date);
        titleList.add(date2);
        titleList.add(date3);

        FragmentTabAdapter adapter = new FragmentTabAdapter(getSupportFragmentManager(), fragmentList, titleList);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        if (slovenijabus_DB.checkIfIn(entryName, exitName)) {
            ImageButton fav_btn = findViewById(R.id.favorite_button);
            fav_btn.setImageResource(R.drawable.heart_full_white);
        }

        final ImageButton swap_direction = findViewById(R.id.show_all_swap_direction);
        swap_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDirection(v);
            }
        });
    }


    public void favoritesButton(View view) {
        Intent intent = getIntent();
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);
        String entryName = slovenijabus_DB.getStationNameFromID(entryStationID);
        String exitName = slovenijabus_DB.getStationNameFromID(exitStationID);
        boolean isIn = slovenijabus_DB.checkIfIn(entryName, exitName);

        ImageButton fav_btn = findViewById(R.id.favorite_button);

        if (isIn) {
            slovenijabus_DB.removeFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_empty_white);
            Toast.makeText(this, getString(R.string.remove_from_favorites), Toast.LENGTH_LONG).show();
        } else {
            slovenijabus_DB.addFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_full_white);
            Toast.makeText(this, getString(R.string.add_to_favorites), Toast.LENGTH_LONG).show();
        }
    }

    public void changeDirection(View view) {
        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        Intent newIntent = new Intent(this, showAllActivity.class);
        newIntent.putExtra(MainActivity.EXTRA_ENTRY, exitStationID);
        newIntent.putExtra(MainActivity.EXTRA_EXIT, entryStationID);
        newIntent.putExtra(MainActivity.EXTRA_DATE, date);
        startActivity(newIntent);
        finish();
    }

    private String dateStringBuilder(int year, int month, int day) {
        month++; // da je pravilen mesec :)
        return day + "." + month + "." + year;
    }

    public String createRequestString(String vstopID, String izstopID, String datum) {
        String request = "VSTOP_ID=" + vstopID + "&IZSTOP_ID=" + izstopID + "&DATUM=" + datum;
        return request;
    }
}
