package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper sInstance;
    private static final String DB_NAME = "SlovenijaBusDB";
    private static final String TABLE_NAME_FAVORITES = "favorites";
    private static final String TABLE_NAME_STATIONS = "stations";
    private static int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE1 = "CREATE TABLE " + TABLE_NAME_FAVORITES +
                " (id INTEGER PRIMARY KEY, " + "entry TEXT, " + "exit TEXT)";
        String CREATE_TABLE2 = "CREATE TABLE " + TABLE_NAME_STATIONS +
                " (id INTEGER PRIMARY KEY, " + "station_id TEXT, " + "station_name TEXT)";
        db.execSQL(CREATE_TABLE1);
        db.execSQL(CREATE_TABLE2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void addFavorite(String entry, String exit) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("entry", entry);
        values.put("exit", exit);

        db.insert(TABLE_NAME_FAVORITES, null, values);
        db.close();
    }

    public void removeFavorite(String entry, String exit) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_NAME_FAVORITES, "entry = ? AND exit = ?", new String[]{entry, exit});
        db.close();
    }

    public void removeFavoriteFromIndex(int index) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_FAVORITES, null, null, null, null, null, null);
        if (cursor.moveToPosition(index)) {
            String rowID = cursor.getString(cursor.getColumnIndex("id"));
            db.delete(TABLE_NAME_FAVORITES, "id=?", new String[]{rowID});
        }
    }

    public ArrayList<HashMap<String, String>> readFavorites() {
        ArrayList<HashMap<String, String>> favorites = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_FAVORITES, null, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> fav = new HashMap<>();
                fav.put("entry", cursor.getString(cursor.getColumnIndex("entry")));
                fav.put("exit", cursor.getString(cursor.getColumnIndex("exit")));
                favorites.add(fav);
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return favorites;
    }

    public boolean checkIfInFavorites(String entry, String exit) {
        if (entry == null || exit == null) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        boolean isIn;
        Cursor cursor = db.query(TABLE_NAME_FAVORITES, new String[]{"id"}, "entry=? AND exit=?", new String[]{entry, exit},
                null, null, null);
        isIn = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isIn;
    }

    public void updateStations(Map<String, String> station_map) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_STATIONS, null, null);

        db.beginTransaction();
        for (String id : station_map.keySet()) {
            ContentValues values = new ContentValues();
            values.put("station_id", id);
            values.put("station_name", station_map.get(id));
            db.insert(TABLE_NAME_STATIONS, null, values);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public Map<String, String> readStationsMap() {
        HashMap<String, String> station_map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_STATIONS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                station_map.put(cursor.getString(cursor.getColumnIndex("station_id")),
                        cursor.getString(cursor.getColumnIndex("station_name")));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return station_map;
    }

    public ArrayList<String> readStationsNames() {
        ArrayList<String> station_names = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME_STATIONS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                station_names.add(cursor.getString(cursor.getColumnIndex("station_name")));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return station_names;
    }

    public String getStationNameFromID(String id) {
        if (id == null) {
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String output;
        Cursor cursor = db.query(TABLE_NAME_STATIONS, new String[]{"station_name"}, "station_id=?", new String[]{id},
                null, null, null);
        if (cursor.moveToFirst()) {
            output = cursor.getString(cursor.getColumnIndex("station_name"));
        } else {
            output = null;
        }
        cursor.close();
        db.close();
        return output;
    }

    public String getStationIDFromName(String name) {
        if (name == null) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        String output;
        Cursor cursor = db.query(TABLE_NAME_STATIONS, new String[]{"station_id"}, "station_name=?", new String[]{name},
                null, null, null);
        if (cursor.moveToFirst()) {
            output = cursor.getString(cursor.getColumnIndex("station_id"));
        } else {
            output = null;
        }
        cursor.close();
        db.close();
        return output;
    }

}
