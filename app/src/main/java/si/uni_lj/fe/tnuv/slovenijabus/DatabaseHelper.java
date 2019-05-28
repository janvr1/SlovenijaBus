package si.uni_lj.fe.tnuv.slovenijabus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper sInstance;
    private static final String DB_NAME = "SlovenijaBusDB";
    private static final String TABLE_NAME = "favorites";
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
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
                "(id INTEGER PRIMARY KEY, " + "entry TEXT, " + "exit TEXT)";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void addFavorite(String entry, String exit) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("entry", entry);
        values.put("exit", exit);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void removeFavorite(String entry, String exit) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_NAME, "entry = ? AND exit = ?", new String[]{entry, exit});
        db.close();
    }

    public ArrayList<HashMap<String, String>> readFavorites() {
        ArrayList<HashMap<String, String>> favorites = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT  * FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> fav = new HashMap<>();
                fav.put("entry", cursor.getString(1));
                fav.put("exit", cursor.getString(2));
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
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE    entry=? AND exit=?";
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

}
