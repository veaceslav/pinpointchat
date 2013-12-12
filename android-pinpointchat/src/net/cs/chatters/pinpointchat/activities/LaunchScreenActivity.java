package net.cs.chatters.pinpointchat.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;


import net.cs.chatters.pinpointchat.R;
import net.cs.chatters.pinpointchat.models.Utils;
import net.cs.chatters.pinpointchat.net.Communicator;

import java.util.HashMap;

public class LaunchScreenActivity extends Activity {


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_screen);

        //instantiate variables from Utils
        Utils.UnreadMessages = new HashMap<String, String>();
        Utils.messageNoSentToUser = new HashMap<String, Integer>();
        Utils.messageNoReceivedFromUser = new HashMap<String, Integer>();
        Utils.appStartTime = System.currentTimeMillis();


        //in case of no internet connectivity the user will be prompted to close the app
        //and check the network status
        boolean online = checkIfOnline();

        //check if logged in
        SharedPreferences mSharedPreferences = getApplicationContext().
                getSharedPreferences(Utils.SharedPrefs, 0);

        //TODO: comment in order to skip the login screen (Daniela)
//        SharedPreferences.Editor e = mSharedPreferences.edit();
//        e.clear();
//        e.commit();

        boolean logged_in = mSharedPreferences.getBoolean(Utils.LOGGED_IN, false);

        if(logged_in & online){
            //get username
            Utils.username = mSharedPreferences.getString(Utils.USERNAME, "");

            //logging in
            Communicator communicator = new Communicator(this);
            communicator.check_username_availability(Utils.username);

            //starting UserListActivity
            Intent intent = new Intent(this, UsersListActivity.class);
            startActivity(intent);
            finish();
        }else if (online){
            //starting MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

    }


    private void showAlertDialog() {


        String title = "Internet connection error";
        String message = "You may need to adjust your network settings";

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);

        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                finish();
            }
        });

        AlertDialog alertDialog = alertBuilder.create();

        alertDialog.show();
    }

    private boolean checkIfOnline() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }

        showAlertDialog();
        return false;
    }

}
