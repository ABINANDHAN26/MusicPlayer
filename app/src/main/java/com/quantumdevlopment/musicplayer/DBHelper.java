package com.quantumdevlopment.arplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.quantumdevlopment.musicplayer.ListAdapter;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {
    private final String FAVOURITE_TABLE = "FavTable";

    public DBHelper(@Nullable Context context) {
        super(context, "fav.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FAVOURITE_TABLE + "(ID INTEGER PRIMARY KEY AUTOINCREMENT,TITLE TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FAVOURITE_TABLE);
        onCreate(db);
    }

    public boolean addFav(int position) {
        String title = ListAdapter.mFiles.get(position).getTitle().toString();
        SQLiteDatabase sd = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("TITLE", title);
        long insert = sd.insert(FAVOURITE_TABLE, null, cv);
        sd.close();
        cv.clear();
        if (insert == -1) {
            return false;
        } else {
            return true;
        }
    }
    public void delFav(int position){
        String title = ListAdapter.mFiles.get(position).getTitle().toString();
        SQLiteDatabase sd = this.getWritableDatabase();
        Cursor cv = sd.rawQuery("DELETE FROM " +FAVOURITE_TABLE+ " WHERE TITLE = '"+title+"';",null);
        cv.close();
        sd.close();
    }
    public ArrayList<String> getFav(){
        ArrayList<String> songLists = new ArrayList<>();
        String query = "SELECT * FROM "+FAVOURITE_TABLE+";";
        SQLiteDatabase sd = this.getReadableDatabase();
        Cursor cv = sd.rawQuery(query,null);
        if(cv.getCount()>0){
            cv.moveToFirst();
            do{
                String title = cv.getString(1);
                songLists.add(title);
            }while (cv.moveToNext());
        }else{
            return null;
        }
        return songLists;
    }
    public boolean isFav(String title) {
        SQLiteDatabase sd = this.getReadableDatabase();
        Cursor cv = sd.rawQuery("SELECT * FROM " + FAVOURITE_TABLE + " WHERE TITLE = '" + title + "';", null);

            if (cv.getCount() > 0) {
                cv.moveToFirst();
                if (title.equalsIgnoreCase(cv.getString(1))) {
                    cv.close();
                    sd.close();
                    return true;
                } else {
                    cv.close();
                    sd.close();
                    return false;
                }
            }

        return false;
    }
}
