package net.cs.chatters.pinpointchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.cs.chatters.pinpointchat.models.Message;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by arthur on 7/18/13.
 */
public class MessagesDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "messages";


    public MessagesDatabase(Context context){
        super(context, DATABASE_NAME, null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase database){
        database.execSQL("CREATE TABLE messages ( date INTEGER, sender TEXT, receiver TEXT, content TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
        database.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(database);
    }

    public void addMessage(Message msg){
        ContentValues values = new ContentValues(4);

        values.put("sender", msg.getSender());
        values.put("receiver", msg.getReceiver());
        values.put("date", msg.getDate());
        values.put("content", msg.getContent());

        Log.i("adaugat", msg.getContent());

        getWritableDatabase().insert("messages","sender",values);
    }

    public ArrayList<Message> getHistory(String user, long date){


        Log.i("adaugat", "getting history");
        ArrayList<Message> messages = new ArrayList<Message>();
        Message message;

        try{
            Cursor allMessages =
                    getReadableDatabase().query(
                            "messages WHERE sender = ?" + " or receiver = ?" + " and date >"+date,
                            new String[] { "sender", "receiver", "date", "content"},
                            null,new String[] {user,user}, null, null, "date ASC"
                    );

            for(allMessages.moveToFirst(); !allMessages.isAfterLast(); allMessages.moveToNext()){
                message = new Message();

                message.setSender(allMessages.getString(0));
                message.setReceiver(allMessages.getString(1));
                message.setDate(allMessages.getLong(2));
                message.setContent(allMessages.getString(3));


                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(message.getDate());
                String hourMin = String.format("[%d:%d]",calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE));
                message.setHourMin(hourMin);

                messages.add(message);
            }

        }catch(Exception e){
            Log.i("Exception",e.getMessage());
        }

        return messages;
    }

    public ArrayList<Message> getMessegesFromNo(String user){

        ArrayList<Message> messages = new ArrayList<Message>();
        Message message;

        try{
            Cursor allMessages =
                    getReadableDatabase().query(
                            "messages WHERE receiver = ?",
                            new String[] { "sender", "receiver", "date", "content"},
                            null,new String[] {user}, null, null, "date ASC"
                    );

            for(allMessages.moveToFirst(); !allMessages.isAfterLast(); allMessages.moveToNext()){
                message = new Message();

                message.setSender(allMessages.getString(0));
                message.setReceiver(allMessages.getString(1));
                message.setDate(allMessages.getLong(2));
                message.setContent(allMessages.getString(3));

                messages.add(message);
            }

        }catch(Exception e){
            Log.i("Exception",e.getMessage());
        }

        return messages;
    }

}