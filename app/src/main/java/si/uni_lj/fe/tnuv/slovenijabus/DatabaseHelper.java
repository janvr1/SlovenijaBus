package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

    public ArrayList<HashMap<String, String>> readFavorites() {
        ArrayList<HashMap<String, String>> favorites = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT  * FROM " + TABLE_NAME_FAVORITES;
        Cursor cursor = db.rawQuery(query, null);
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

    public boolean checkIfIn(String entry, String exit) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean isIn;
        String query = "SELECT * FROM " + TABLE_NAME_FAVORITES + " WHERE entry=? AND exit=?";
        Cursor cursor = db.rawQuery(query, new String[]{entry, exit});
        if (cursor.getCount() > 0) {
            isIn = true;
        } else {
            isIn = false;
        }
        cursor.close();
        db.close();
        return isIn;
    }

    public void updateStations(Map<String, String> station_map) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_STATIONS, null, null);

        for (String id : station_map.keySet()) {
            ContentValues values = new ContentValues();
            values.put("station_id", id);
            values.put("station_name", station_map.get(id));
            db.insert(TABLE_NAME_STATIONS, null, values);
        }
        db.close();
    }

    public Map<String, String> readStationsMap() {
        HashMap<String, String> station_map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT  * FROM " + TABLE_NAME_STATIONS;
        Cursor cursor = db.rawQuery(query, null);
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
        String query = "SELECT  * FROM " + TABLE_NAME_STATIONS;
        Cursor cursor = db.rawQuery(query, null);
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
        SQLiteDatabase db = this.getReadableDatabase();
        String output;
        String query = "SELECT * FROM " + TABLE_NAME_STATIONS + " WHERE station_id=?";
        Cursor cursor = db.rawQuery(query, new String[]{id});
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
        SQLiteDatabase db = this.getReadableDatabase();
        String output;
        String query = "SELECT * FROM " + TABLE_NAME_STATIONS + " WHERE station_name=?";
        Cursor cursor = db.rawQuery(query, new String[]{name});
        if (cursor.moveToFirst()) {
            output = cursor.getString(cursor.getColumnIndex("station_id"));
        } else {
            output = null;
        }
        cursor.close();
        db.close();
        return output;
    }

    public String tableToString(SQLiteDatabase db, String tableName) {
        Log.d("", "tableToString called");
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows = db.rawQuery("SELECT * FROM " + tableName, null);
        tableString += cursorToString(allRows);
        return tableString;
    }

    public String cursorToString(Cursor cursor) {
        String cursorString = "";
        if (cursor.moveToFirst()) {
            String[] columnNames = cursor.getColumnNames();
            for (String name : columnNames)
                cursorString += String.format("%s ][ ", name);
            cursorString += "\n";
            do {
                for (String name : columnNames) {
                    cursorString += String.format("%s ][ ",
                            cursor.getString(cursor.getColumnIndex(name)));
                }
                cursorString += "\n";
            } while (cursor.moveToNext());
        }
        return cursorString;
    }

}
