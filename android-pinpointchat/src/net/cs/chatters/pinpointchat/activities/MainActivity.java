package net.cs.chatters.pinpointchat.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.cs.chatters.pinpointchat.models.Utils;
import net.cs.chatters.pinpointchat.net.Communicator;
import net.cs.chatters.pinpointchat.R;

import java.util.Timer;


public class MainActivity extends Activity {

    String username = "";

    public final static int USER_TAKEN = 0;
    public final static int USER_AVAILABLE = 1;
    public final static int EMPTY_FIELD = 2;
    public final static int INTERNAL_PROBLEM = 3;

    private Communicator communicator = new Communicator(this);

    private void startSession() {


        //saving username in shared preferences
        SharedPreferences mSharedPreferences = getApplicationContext().
                                                    getSharedPreferences(Utils.SharedPrefs, 0);
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.putString(Utils.USERNAME, username);
        e.putBoolean(Utils.LOGGED_IN, true);
        e.commit(); // save changes

        Utils.username = username;

        //starting UserListActivity
        Intent intent = new Intent(this, UsersListActivity.class);
        startActivity(intent);
        finish();

    }

    private void showAlertDialog(String message) {


        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(message);

        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertBuilder.create();

        alertDialog.show();
    }

    private void trySignIn() {
        EditText user_field = (EditText) findViewById(R.id.user_field);
        String desiredUsername = user_field.getText().toString();

        int check_availability_result = communicator.check_username_availability(desiredUsername);

        if (check_availability_result == USER_TAKEN) {
            showAlertDialog("Username taken!");
        } else if (check_availability_result == EMPTY_FIELD) {
            showAlertDialog("Please insert an username");
            //todo: uncomment
//        } else if (check_availability_result == INTERNAL_PROBLEM) {
//            showAlertDialog("Sorry. The application had some communication errors.");
//            finish();
        } else {
            username = desiredUsername;
            startSession();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        communicator = new Communicator(this);
        Button sign_in_button = (Button) findViewById(R.id.sing_in_button);
        sign_in_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    trySignIn();
            }
        });

    }


}
