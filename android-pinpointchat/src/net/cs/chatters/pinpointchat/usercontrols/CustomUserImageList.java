package net.cs.chatters.pinpointchat.usercontrols;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.cs.chatters.pinpointchat.R;
import net.cs.chatters.pinpointchat.models.UserData;
import net.cs.chatters.pinpointchat.activities.ChatActivity;
import net.cs.chatters.pinpointchat.models.Utils;

import java.util.ArrayList;

/**
 * Created by arthur on 7/11/13.
 */
public class CustomUserImageList extends BaseAdapter {

    private static final int BITMAP_S1 = 70;
    private static final int BITMAP_S2 = 70;

    public static ArrayList<UserData> usersList;
    private LayoutInflater layoutInflater;
    private Context context;


    public CustomUserImageList(Context context, ArrayList<UserData> userslist) {
        usersList = userslist;
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public void changeUsersList(ArrayList<UserData> userslist){
        usersList = userslist;
        if (usersList != null )
        {
        	this.notifyDataSetChanged();
        }
    }

    public int getCount() {
        return usersList.size();
    }

    public Object getItem(int position) {
        return usersList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }


    public void startChatActivity(String interlocutor){

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(net.cs.chatters.pinpointchat.models.Utils.INTERLOCUTOR, interlocutor);
        context.startActivity(intent);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ImageHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_elm_layout, null);

            holder = new ImageHolder();
            holder.nameView = (TextView) convertView.findViewById(R.id.nameView);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
            holder.distanceView=(TextView) convertView.findViewById(R.id.distanceView);
            holder.notificationView = (ImageView) convertView.findViewById(R.id.notification);

            convertView.setTag(holder);
        } else
            holder = (ImageHolder) convertView.getTag();

        holder.nameView.setTextColor(Color.BLACK);
        holder.nameView.setText(usersList.get(position).getName());
        holder.notificationView.setImageDrawable(null);
        holder.distanceView.setText("\n~" + Integer.toString(usersList.get(position).getDistanceFromUser()) + " meters away");
        holder.imageView.getBackground().setColorFilter(context.getResources().getColor(R.color.culoare1 +
                                                        Math.abs(usersList.get(position).getName().hashCode())% Utils.numberOfDefinedColors),
                                                        PorterDuff.Mode.MULTIPLY);

        if (Utils.UnreadMessages.get(usersList.get(position).getName()) == "1") {
            holder.nameView.setTextColor(Color.RED);
            holder.notificationView.setImageResource(R.drawable.notification);
        }

        return convertView;
    }

    private static class ImageHolder {
        public TextView nameView;
        public TextView distanceView;
        public ImageView imageView;
        public ImageView notificationView;
    }

}