package net.cs.chatters.pinpointchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EmoticonDb extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "emoticondb";

    public EmoticonDb(Context context){
        super(context, DATABASE_NAME, null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase database){
        database.execSQL("CREATE TABLE emoticondb (user TEXT, emoticon TEXT, unique(user) ON CONFLICT replace);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
        database.execSQL("DROP TABLE IF EXISTS emoticondb");
        onCreate(database);
    }

    public void addUser(String user, String emoticon){
        ContentValues values = new ContentValues(2);

        values.put("user", user);
        values.put("emoticon", emoticon);

        Log.i("adaugat","Noua mapare");

        getWritableDatabase().insert(DATABASE_NAME,"user",values);
    }


    public String getEmoticonFromNo(String user){

    	String emoticonString = new String();

        try{
            Cursor allMessages =
                    getReadableDatabase().query(
                            "emoticondb WHERE user = ?",
                            new String[] { "user", "emoticon"},
                            null,new String[] {user}, null, null, "user ASC"
                    );

            for(allMessages.moveToFirst(); !allMessages.isAfterLast(); allMessages.moveToNext()){
            	emoticonString = allMessages.getString(1);
            }

        }catch(Exception e){
            Log.i("Exception",e.getMessage());
        }

        return emoticonString;
    }

}
