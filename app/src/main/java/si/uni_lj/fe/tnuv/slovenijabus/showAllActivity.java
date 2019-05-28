package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class showAllActivity extends AppCompatActivity {

    DatabaseHelper favorites_db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all);

        Toolbar myToolbar = findViewById(R.id.show_all_toolbar);
        setSupportActionBar(myToolbar);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));
        }
        favorites_db = DatabaseHelper.getInstance(this);

        Intent intent = getIntent();
        String date = intent.getStringExtra(MainActivity.EXTRA_DATE);
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);

        String request_data = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date;

        String date2 = null, date3 = null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date date1 = sdf.parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(date1);
            c.add(Calendar.DAY_OF_MONTH, 1);
            date2 = sdf.format(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 1);
            date3 = sdf.format(c.getTime());
        } catch (Exception e) {
            Log.d("parse exception", "something no worky worky");
        }

        String request_data2 = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date2;
        String request_data3 = "VSTOP_ID=" + entryStationID + "&IZSTOP_ID=" + exitStationID + "&DATUM=" + date3;

        String entryName = MainActivity.stations_map.get(entryStationID);
        String exitName = MainActivity.stations_map.get(exitStationID);

        TextView vstop = findViewById(R.id.show_all_vstop);
        vstop.setText(entryName);
        TextView izstop = findViewById(R.id.show_all_izstop);
        izstop.setText(exitName);

        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.setOffscreenPageLimit(2);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(timetableFragment.newInstance(request_data));
        fragmentList.add(timetableFragment.newInstance(request_data2));
        fragmentList.add(timetableFragment.newInstance(request_data3));

        List<String> titleList = new ArrayList<>();
        titleList.add(date);
        titleList.add(date2);
        titleList.add(date3);

        FragmentTabAdapter adapter = new FragmentTabAdapter(getSupportFragmentManager(), fragmentList, titleList);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

/*        if (checkIfInFavorites(readFavorites(), entryStationID, exitStationID) > -1) {
            ImageButton fav_btn = findViewById(R.id.favorite_button);
            fav_btn.setImageResource(R.drawable.heart_full_white);
        }*/
        Log.d("showAllActivity", entryName + " " + exitName);
        if (favorites_db.checkIfIn(entryName, exitName)) {
            ImageButton fav_btn = findViewById(R.id.favorite_button);
            fav_btn.setImageResource(R.drawable.heart_full_white);
        }
    }

    public void favoritesButton(View view) {
        //ArrayList<HashMap<String, String>> favorites = readFavorites();

        Intent intent = getIntent();
        String entryStationID = intent.getStringExtra(MainActivity.EXTRA_ENTRY);
        String exitStationID = intent.getStringExtra(MainActivity.EXTRA_EXIT);
        String entryName = MainActivity.stations_map.get(entryStationID);
        String exitName = MainActivity.stations_map.get(exitStationID);
        //int index = checkIfInFavorites(favorites, entryStationID, exitStationID);
        boolean isIn = favorites_db.checkIfIn(entryName, exitName);

        ImageButton fav_btn = findViewById(R.id.favorite_button);

        if (isIn) {
            favorites_db.removeFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_empty_white);
            Toast.makeText(this, getString(R.string.remove_from_favorites), Toast.LENGTH_LONG).show();
        } else {
            favorites_db.addFavorite(entryName, exitName);
            fav_btn.setImageResource(R.drawable.heart_full_white);
            Toast.makeText(this, getString(R.string.add_to_favorites), Toast.LENGTH_LONG).show();
        }
        dumpDBtoLog();

/*        if (index == -1) { //dodamo
            HashMap<String, String> newMap = new HashMap<>();
            newMap.put("from", MainActivity.stations_map.get(entryStationID));
            newMap.put("to", MainActivity.stations_map.get(exitStationID));

            favorites.add(newMap);
            writeFavorites(favorites);
            fav_btn.setImageResource(R.drawable.heart_full_white);
            Toast.makeText(this, getString(R.string.add_to_favorites), Toast.LENGTH_LONG).show();
        } else { //odstranimo
            favorites.remove(index);
            writeFavorites(favorites);
            fav_btn.setImageResource(R.drawable.heart_empty_white);
            Toast.makeText(this, getString(R.string.remove_from_favorites), Toast.LENGTH_LONG).show();
        }*/

    }

    public int checkIfInFavorites(ArrayList<HashMap<String, String>> favorites,
                                  String entryStationID, String exitStationID) {

        // vrne indeks elementa če je že v favorites, če ni vrne -1
        int index = -1;
        for (int j = 0; j < favorites.size(); j++) {
            HashMap<String, String> map = favorites.get(j);
            if (map.get("from").equals(MainActivity.stations_map.get(entryStationID)) &&
                    map.get("to").equals(MainActivity.stations_map.get(exitStationID))) {
                index = j;
            }
        }
        return index;
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

    public void dumpDBtoLog() {
        for (HashMap<String, String> hm : favorites_db.readFavorites()) {
            Log.d("db", hm.toString());
        }
    }
}
