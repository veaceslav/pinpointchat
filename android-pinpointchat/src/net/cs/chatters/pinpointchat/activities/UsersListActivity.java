package net.cs.chatters.pinpointchat.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import net.cs.chatters.pinpointchat.models.Utils;
import net.cs.chatters.pinpointchat.net.Communicator;
import net.cs.chatters.pinpointchat.net.UserPositionUpdater;
import net.cs.chatters.pinpointchat.usercontrols.CustomUserImageList;
import net.cs.chatters.pinpointchat.models.UserData;
import net.cs.chatters.pinpointchat.R;

import java.util.ArrayList;
import java.util.Set;


public class UsersListActivity extends Activity {

    CustomUserImageList usersListAdapter;
    protected long timeOfLastUpdate = 0;

    ListView myListView ;
    protected Communicator communicator = new Communicator(this);
    private UserPositionUpdater userPositionUpdater;


    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.userslistlayout);

        userPositionUpdater = new UserPositionUpdater((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        userPositionUpdater.execute();

        usersListAdapter = new CustomUserImageList(this,new ArrayList<UserData>());

        myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(usersListAdapter);

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            public void onItemClick(AdapterView<?> parent, View v, int position, long id){
                Utils.UnreadMessages.put(CustomUserImageList.usersList.get(position).getName(), "0");
                UserData userData = (UserData) myListView.getItemAtPosition(position);
                //communicator.fetchMessages(userData.getName());
                usersListAdapter.startChatActivity(userData.getName());
            }
        });
    }


    public void onResume() {
        // Always call the superclass method first
        super.onResume();
        usersListAdapter.notifyDataSetChanged();

        long currentTime = System.currentTimeMillis();

        if(currentTime - Utils.timeOfLastReceivedMessage > Utils.refreshUsersList){
            //TODO: uncomment
           // communicator.timeOfLastReceivedMessageExpired();
            Utils.timeOfLastReceivedMessage = currentTime;
        }

        if(currentTime - timeOfLastUpdate > Utils.refreshUsersList){
            refreshUsersList();
            timeOfLastUpdate = currentTime;
        }
    }

    public void onDestroy(){
        super.onDestroy();
        userPositionUpdater.setOffline();
        userPositionUpdater.cancel(true);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.incognitobutton:
                incognitoMode(true);
                return true;
            case R.id.logoutbutton:
                LogOut();
                return true;
            case R.id.updatepositionbutton:
                userPositionUpdater.updateNow();
                return true;
            case R.id.refreshuserslistbutton:
                refreshUsersList();
            default: return false;
        }
    }

    private void LogOut(){
        communicator.delete_user(Utils.username);

        userPositionUpdater.cancel(true);

        clearPreferences();

        Intent intent = new Intent(this, LaunchScreenActivity.class);
        startActivity(intent);

        finish();
    }

    private void incognitoMode(boolean activate){
        if(activate){
            ;
        }
        else{
            ;
        }
    }

    private void clearPreferences(){
        SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(Utils.SharedPrefs, 0);
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.clear();
        e.commit();
    }

    private void refreshUsersList(){
        userPositionUpdater.updateNow();
    //TODO: uncomment
    //ArrayList<UserData> usersList = communicator.getUsers(new UserData(Utils.username).getName());


        //~~~~~~~ stub ~~~~~~//
        ArrayList<UserData> usersList = new ArrayList<UserData>();
        UserData ud = new UserData();
        ud.setName("Daniela");
        usersList.add(ud);

        usersListAdapter.changeUsersList(usersList);
    }

}