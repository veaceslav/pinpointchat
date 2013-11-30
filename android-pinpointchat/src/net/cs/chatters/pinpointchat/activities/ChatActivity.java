package net.cs.chatters.pinpointchat.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import net.cs.chatters.pinpointchat.database.MessagesDatabase;
import net.cs.chatters.pinpointchat.models.Message;
import net.cs.chatters.pinpointchat.models.Utils;
import net.cs.chatters.pinpointchat.net.Communicator;
import net.cs.chatters.pinpointchat.R;

import java.util.ArrayList;

public class ChatActivity extends Activity {

    String username;
    String interlocutor;
    Communicator communicator;

    ArrayList<String> messages;
    ArrayAdapter<String> adapter;
    ListView listview;
    private MessagesDatabase db;

    private MessageReceiver messageReceiver;
    private IntentFilter mOnMessageFilter;

    private int Smiling = 0;
    private int Laughing = 0;
    private int Happy = 0;
    private int Sad = 0;
    private int Crying = 0;

    private void processEmoticons(String message){
        Laughing += getOccurrences(message,":))") + getOccurrences(message,":-))");
        Smiling  += getOccurrences(message,":)")  + getOccurrences(message,":-)");
        Crying   += getOccurrences(message,":((") + getOccurrences(message,":-((");
        Happy    += getOccurrences(message,":D")  + getOccurrences(message,":-D");
        Sad      += getOccurrences(message,":(")  + getOccurrences(message,":-(");
    }

    private int getOccurrences(String message, String s){

        int occurrences = 0;

        while (true){
            int i = message.indexOf(s);
            if(i == -1)
                break;
            occurrences++;
        }

        return occurrences;
    }


    public ChatActivity(){
        communicator = new Communicator(this);
    }


    private void createList() {

        messages = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, R.layout.chat_list_item, messages);
        listview = (ListView) findViewById(R.id.msg_list);
        listview.setAdapter(adapter);

    }

    private void scrollDown() {

        listview.setSelection(adapter.getCount() - 1);

    }

    public void displayMessagesToScreen(String interlocutor){

        //Utils.appStartTime <=> time on which the app has started
        db = new MessagesDatabase(getApplicationContext());
        ArrayList<Message> messagesInDB = db.getHistory(interlocutor, Utils.appStartTime);
        db.close();
        Log.i("ChatActivity:", "Messages in database:"+messagesInDB.size());


        for (int i = messages.size(); i<messagesInDB.size(); i++){

            Message msg = messagesInDB.get(i);
            String message = String.format("%s %s: %s", msg.getHourMin(), msg.getSender(), msg.getContent());
            messages.add(message);
            adapter.notifyDataSetChanged();
            scrollDown();
        }

    }

    private void sendMsg() {

        //get message from text box
        EditText textBox = (EditText) findViewById(R.id.text_box);
        String message = textBox.getText().toString();
        if(message.length() != 0){
            textBox.setText("");

        int current_nr;
        //save the nr of messages sent to that user
        if(Utils.messageNoSentToUser.containsKey(interlocutor)){
           current_nr = Utils.messageNoSentToUser.get(interlocutor);
            Utils.messageNoSentToUser.put(interlocutor,current_nr+1);
        }else{
            Utils.messageNoSentToUser.put(interlocutor,1);
            current_nr = 1;
        }

            communicator.sendMessageToServer(Utils.username, interlocutor, message);
            Message msg = new Message (Utils.username, interlocutor, message, true, current_nr);
            db.addMessage(msg);
            db.close();
            displayMessagesToScreen(interlocutor);
        }
        else
            Toast.makeText(this,"Please enter a message",Toast.LENGTH_LONG).show();
    }

    protected void onCreate(Bundle savedInstanceState) {

        Log.i("ChatActivity", "starting");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_screen);

        mOnMessageFilter = new IntentFilter();
        mOnMessageFilter.addAction(Utils.ACTION_NEW_MESSAGE);
        messageReceiver = new MessageReceiver();

        Intent intent = getIntent();
        interlocutor = intent.getStringExtra(Utils.INTERLOCUTOR);

        //get username

        SharedPreferences mSharedPreferences = getApplicationContext().
                getSharedPreferences(Utils.SharedPrefs, 0);
        username = mSharedPreferences.getString(Utils.USERNAME, "");
        Log.i("Username", username);

        createList();

        displayMessagesToScreen(interlocutor);

        //set button listener
        Button send_button = (Button) findViewById(R.id.send_button);
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsg();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        communicator.notifyInOutOfChat(true, Utils.username, interlocutor);
        registerReceiver(messageReceiver, mOnMessageFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        communicator.notifyInOutOfChat(false, Utils.username, interlocutor);
        unregisterReceiver(messageReceiver);
    }

    public class MessageReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ChatActivity","newmsgs");
            displayMessagesToScreen(interlocutor);
        }
    }
}


