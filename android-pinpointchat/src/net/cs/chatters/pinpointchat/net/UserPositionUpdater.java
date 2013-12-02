package net.cs.chatters.pinpointchat.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.cs.chatters.pinpointchat.models.Utils;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

public class UserPositionUpdater extends AsyncTask<String, Void, String> {

    private LocationManager locationManager;

    public UserPositionUpdater (LocationManager locationmanager){
        locationManager = locationmanager;
    }

    public void updateNow(LocationManager _locationmanager){
    	new UserPositionUpdater(_locationmanager).execute();
    }

    public void setOffline(){
        double coord[] = {0,0};
      //  updatePosition(coord);
    }

    public String doInBackground(String ... params){
        try{
            double [] coord = getGPS();
            Utils.UserLat = coord[0];
            Utils.UserLng = coord[1];
            String uri = Utils.ServerURL + "/updatelocation"+ "?username=" + Utils.username +
                    "&latitude=" + Double.toString(coord[0]) + "&longitude=" +
                    Double.toString(coord[1]);

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(uri);

            try {
                httpClient.execute(httpPost);
            }catch (UnsupportedEncodingException e) {
                Log.e("UnsupportedEncodingException", e.getMessage());
                e.printStackTrace();

            } catch (ClientProtocolException e) {
                Log.e("ClientProtocolException", e.getMessage());
                e.printStackTrace();

            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                e.printStackTrace();

            }
            Thread.sleep(Utils.userPositionUpdaterDELAY);
        }catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

/*            if(isCancelled()){
                return "canceled";
            }*/

        return null;
    }


    public double[] getGPS() {
        double[] gpsLocation = new double[2];
        //List<String> providers = locationManager.getProviders(true);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = locationManager.getBestProvider(criteria, true);

        Location location = locationManager.getLastKnownLocation(provider);
        if(location != null) {
            gpsLocation[0] = location.getLatitude();
            gpsLocation[1] = location.getLongitude();
        }

        return gpsLocation;
    }


}