package net.cs.chatters.pinpointchat.net;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import net.cs.chatters.pinpointchat.activities.MainActivity;
import net.cs.chatters.pinpointchat.database.MessagesDatabase;
import net.cs.chatters.pinpointchat.models.Message;
import net.cs.chatters.pinpointchat.models.UserData;
import net.cs.chatters.pinpointchat.models.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class Communicator {

    Context context;

    public Communicator(Context context){
        this.context=context;
    }

    public void notifyInOutOfChat(Boolean _inChat, String username, String interlocutor){

        String inChat = _inChat.toString();

        new InOutOfChatNotifier().execute(inChat, username, interlocutor);

    }

    public ArrayList<UserData> getUsers(String username){

        ArrayList<UserData> userData = new ArrayList<UserData>();
        try{
           userData =  (new UserFetcher().execute(username)).get();
        }

        catch (Exception e){
           Log.i("UserFetcher", e.getMessage());
        }

        return userData;
    }


    public void timeOfLastReceivedMessageExpired(){
        //pull al messages from server (ordered by MsgNo)
        String unreceivedMessagesJSON = getUnreceivedMessagesJSON();

        ArrayList<Message> unreceivedMessages = getUnreceivedMessages(unreceivedMessagesJSON);

        MessagesDatabase messagesDatabase = new MessagesDatabase(context);

        for(int i = 0; i < unreceivedMessages.size(); i++){

            Message message = unreceivedMessages.get(i);
            messagesDatabase.addMessage(message);
            Utils.UnreadMessages.put(message.getSender(),"1");

            if(Utils.messageNoReceivedFromUser.containsKey(message.getSender())){

                if(message.msgNo==Utils.messageNoReceivedFromUser.get(message.getSender())+1){
                }
            }
            else ;
        }

        messagesDatabase.close();
    }

    private ArrayList<Message> getUnreceivedMessages(String messagesJSON){

        ArrayList<Message> messages = new ArrayList<Message>();

        try {
            JSONObject responseData = new JSONObject(messagesJSON);
            JSONArray ResultsArray = responseData.getJSONArray("results");
            for (int i = 0; i < ResultsArray.length(); i++) {
                try {
                    Message message = new Message();
                    message.setReceiver(Utils.username);
                    message.setSender(ResultsArray.getJSONObject(i).getString("sender"));
                    message.msgNo = (ResultsArray.getJSONObject(i).getInt("msgNo"));
                    message.setContent(ResultsArray.getJSONObject(i).getString("content"));

                    messages.add(message);
                } catch( JSONException e){
                    e.printStackTrace();
                }
            }
        } catch( JSONException ex){
            ex.printStackTrace();
        }

        return messages;
    }

    private String getUnreceivedMessagesJSON(){
        String uri = Utils.ServerURL + "/getunreceivedmessages" + "?username=" + Utils.username; // parametru user.getName()
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);
        String unreceivedMessagesJSON = new String();
        try {

            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {

                unreceivedMessagesJSON = EntityUtils.toString(entity);//unreceiverMessagesJSON e un json
                return unreceivedMessagesJSON;
            }

        } catch (UnsupportedEncodingException e) {

            Log.e("UnsupportedEncodingException", e.getMessage());
            e.printStackTrace();

        } catch (ClientProtocolException e) {

            Log.e("ClientProtocolException", e.getMessage());
            e.printStackTrace();

        } catch (IOException e) {

            Log.e("IOException", e.getMessage());
            e.printStackTrace();

        }

        return unreceivedMessagesJSON;
    }


    private void checkNotNull(Object reference, String name) {
        if (reference == null) {
            throw new NullPointerException("Field " + name + " is null");
        }
    }


    public void setRegID(){

        checkNotNull(Utils.ServerURL, "GCMServerRegisterURL");
        checkNotNull(Utils.SENDER_ID, "SENDER_ID");

        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);


        Utils.regid = GCMRegistrar.getRegistrationId(context);

        if(Utils.regid.length()==0){
            GCMRegistrar.register(context, Utils.SENDER_ID);
        }

        Utils.regid = GCMRegistrar.getRegistrationId(context);
        Log.i("REGID",""+Utils.regid);
    }



    public int check_username_availability(String username) {

        if (username.length() == 0)
            return MainActivity.EMPTY_FIELD;

        boolean result = false;

        try {
            setRegID();
            if(Utils.regid.equalsIgnoreCase("")){
                return MainActivity.INTERNAL_PROBLEM;
            }
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            result = (new UserSubscriber().execute(username, Utils.regid)).get();
            threadSet = Thread.getAllStackTraces().keySet();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (result) {
            return MainActivity.USER_AVAILABLE;
        }

        return MainActivity.USER_TAKEN;
    }


    public boolean delete_user(String username) {

        boolean result = false;

        try {
            result = (new UserEraser().execute(username)).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return result;
    }

    public ArrayList<String> fetchMessages (String username) {

        if(!Utils.regid.equals(GCMRegistrar.getRegistrationId(context)))
            Log.i("Problem:", "different regids");

        ArrayList<String> messages = new ArrayList<String>();

        try {
            messages = (new MessageFetcher().execute(username)).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public void sendMessageToServer(String sender, String receiver, String content) {

        if(!Utils.regid.equals(GCMRegistrar.getRegistrationId(context)))
            Log.i("Problem:", "different regids");

        new MessageSender().execute(sender, receiver, content);
    }

}


class UserFetcher extends AsyncTask<String, Void, ArrayList<UserData>> {

    protected ArrayList<UserData> doInBackground(String... credentials){

        ArrayList<UserData> Users = new ArrayList<UserData>();
        String uri = Utils.ServerURL + "/getusers" + "?username=" + credentials[0];
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {

            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {

                String jsonString = EntityUtils.toString(entity);

                JSONObject responseData = new JSONObject(jsonString);
                JSONArray ResultsArray = responseData.getJSONArray("results");
                for (int i = 0; i < ResultsArray.length(); i++) {
                    try {
                        UserData tempUser = new UserData();
                        tempUser.setName(ResultsArray.getJSONObject(i).getString("username"));
                        tempUser.setDistanceFromUser(ResultsArray.getJSONObject(i).getInt("distance"));

                        //if(!tempUser.getName().equals(Utils.username)){
                            Users.add(tempUser);
                        //}
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                return Users;

            }

        } catch (UnsupportedEncodingException e) {

            Log.e("UnsupportedEncodingException", e.getMessage());
            e.printStackTrace();

        } catch (ClientProtocolException e) {

            Log.e("ClientProtocolException", e.getMessage());
            e.printStackTrace();

        } catch (IOException e) {

            Log.e("IOException", e.getMessage());
            e.printStackTrace();

        } catch (JSONException e) {

            Log.e("JSONException", e.getMessage());
            e.printStackTrace();

        }

        return null;

    }

}


class UserSubscriber extends AsyncTask<String, Void, Boolean> {

    protected Boolean doInBackground(String... credentials){


        final String user = credentials[0];
        final String regid = credentials[1];

        //format url
        String uri = Utils.ServerURL +"/register?username=";
        uri += user;
        uri += "&regid="+regid;

        Log.i("ServerURL:",uri);


        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {
        // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            int exitCode = response.getStatusLine().getStatusCode();

            if (exitCode == Utils.OK) {
                    return true;
            } else if (exitCode == Utils.Not_Acceptable) {
                    return false;
            }


        } catch (ClientProtocolException e) {
            Log.i("Client:", e.getMessage());
        } catch (IOException e) {
            Log.i("I/O:", e.getMessage());
        }


        return false;

    }

}

class InOutOfChatNotifier extends AsyncTask<String, Void, Boolean> {

    protected Boolean doInBackground(String... data){


        final String inChat = data[0];
        final String username = data[1];
        final String interlocutor = data[2];

        //building the request
        String uri = Utils.ServerURL +"/in_out_chat?inChat=";
        uri += inChat;
        uri += "&username="+username;
        uri += "&interlocutor="+interlocutor;


        if(Utils.messageNoReceivedFromUser.containsKey(interlocutor)){
            uri += "&msgNo=" + Utils.messageNoReceivedFromUser.get(interlocutor);

        }else{
            uri += "&msgNo=" + "0";
            Utils.messageNoReceivedFromUser.put(interlocutor,0);
        }

        Log.i("I got:",""+Utils.messageNoReceivedFromUser.get(interlocutor) + " messages");

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            int exitCode = response.getStatusLine().getStatusCode();

            if (exitCode == Utils.OK) {
               return true;
            } else if (exitCode == Utils.Not_Acceptable) {
               return false;
            }


        } catch (ClientProtocolException e) {
            Log.e("Client:", e.getMessage());
        } catch (IOException e) {
            Log.e("I/O:", e.getMessage());
        }


        return false;

    }
}


class UserEraser extends AsyncTask<String, Void, Boolean> {

    protected Boolean doInBackground(String... usernames){


        final String user = usernames[0];

        String uri = Utils.ServerURL +"/delete?username=";
        uri += user;

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            int exitCode = response.getStatusLine().getStatusCode();

            if (exitCode == Utils.OK) {
                return true;
            } else if (exitCode == Utils.Not_Acceptable) {
                return false;
            }


        } catch (ClientProtocolException e) {
            Log.e("Client:", e.getMessage());
        } catch (IOException e) {
            Log.e("I/O:", e.getMessage());
        }


        return false;

        }
    }


class MessageFetcher extends AsyncTask<String, Void, ArrayList<String>> {

    protected ArrayList<String> doInBackground(String... usernames){

        final String sender = usernames[0];

        String uri = Utils.ServerURL +"/fetch_msgs?sender=";
        uri += sender;
        uri += "&receiver="+Utils.username;

        if(Utils.messageNoReceivedFromUser.containsKey(sender)){
            uri += "&msgNo=" + Utils.messageNoReceivedFromUser.get(sender);
        }else{
            uri += "&msgNo=" + "0";
            Utils.messageNoReceivedFromUser.put(sender,0);
        }

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            int exitCode = response.getStatusLine().getStatusCode();

//            Utils.gotMessages = false;
//            while (!Utils.gotMessages){}


        } catch (ClientProtocolException e) {
            Log.e("Client:", e.getMessage());
        } catch (IOException e) {
            Log.e("I/O:", e.getMessage());
        }


        return Utils.messages;

    }
}



class MessageSender extends AsyncTask<String, Void, Boolean> {

    protected Boolean doInBackground(String... data){

        final String sender = data[0];
        final String receiver = data[1];
        final String content = data[2];


        //building the request
        String uri = Utils.ServerURL +"/send_msg?";
        uri += "sender="+sender;
        uri += "&receiver="+receiver;
        uri += "&msgNo_sent="+Utils.messageNoSentToUser.get(receiver);
        uri += "&msgNo_received="+Utils.messageNoReceivedFromUser.get(receiver);

        try {
            uri = uri + "&content=" + URLEncoder.encode(content, "UTF-8");
            Log.i("Encoding", uri);
        } catch (UnsupportedEncodingException e) {
            Log.i("Encoding error:", e.getMessage());
        }


        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(uri);

        try {
            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);

            int exitCode = response.getStatusLine().getStatusCode();

            if (exitCode == Utils.OK) {
                return true;
            } else if (exitCode == Utils.Not_Acceptable) {
                return false;
            }


        } catch (ClientProtocolException e) {
            Log.e("Client:", e.getMessage());
        } catch (IOException e) {
            Log.e("I/O:", e.getMessage());
        }


        return false;

    }


}








