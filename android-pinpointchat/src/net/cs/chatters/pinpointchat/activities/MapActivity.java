package net.cs.chatters.pinpointchat.activities;

import java.util.ArrayList;

import net.cs.chatters.pinpointchat.R;
import net.cs.chatters.pinpointchat.models.UserData;
import net.cs.chatters.pinpointchat.models.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity {
  static final LatLng HAMBURG = new LatLng(53.558, 9.927);
  static final LatLng KIEL = new LatLng(53.551, 9.993);
  private GoogleMap map;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.map_layout);
    map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
        .getMap();

    Intent intent = getIntent();
    @SuppressWarnings("unchecked")
	ArrayList<UserData> userList = (ArrayList<UserData>)intent.getSerializableExtra("usersList");
    LatLng ownerPoz = new LatLng(Utils.UserLat, Utils.UserLng);
    addMarker(Utils.username, ownerPoz);

    for(int i = 0; i < userList.size(); i++)
    {
    	addMarker(userList.get(i).getName(),new LatLng( userList.get(i).lat, userList.get(i).lng ));
    }
    /**
    Marker kiel = map.addMarker(new MarkerOptions()
        .position(KIEL)
        .title("Kiel")
        .snippet("Kiel is cool")
        .icon(BitmapDescriptorFactory
            .fromResource(R.drawable.ic_launcher)));
	*/
    // Move the camera instantly to hamburg with a zoom of 15.
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(ownerPoz, 20));

    // Zoom in, animating the camera.
    map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
  }

  void addMarker(String name, LatLng poz)
  {
	  map.addMarker(new MarkerOptions().position(poz).title(name));
  }


}