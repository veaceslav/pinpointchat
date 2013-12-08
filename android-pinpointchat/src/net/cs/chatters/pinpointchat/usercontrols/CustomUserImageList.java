package net.cs.chatters.pinpointchat.usercontrols;

import java.util.ArrayList;

import net.cs.chatters.pinpointchat.R;
import net.cs.chatters.pinpointchat.activities.ChatActivity;
import net.cs.chatters.pinpointchat.database.EmoticonDb;
import net.cs.chatters.pinpointchat.models.UserData;
import net.cs.chatters.pinpointchat.models.Utils;
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


public class CustomUserImageList extends BaseAdapter {
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
        String emoticonName;
        EmoticonDb emoticonDb = new EmoticonDb(context);
        emoticonName = emoticonDb.getEmoticonFromNo(usersList.get(position).getName());
        emoticonDb.close();

        //Log.i("EMO DB", "Got emoticon name for " + usersList.get(position).getName() + ": emoticon: " + emoticonName);
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

        /** Set Avatars based on emoticon data from EmoticonDb **/
        if(emoticonName.isEmpty())
        	holder.imageView.setImageResource(R.drawable.straightface);

        if(emoticonName.equals("Smiling"))
        	holder.imageView.setImageResource(R.drawable.smile);

        if(emoticonName.equals("Tongue"))
        	holder.imageView.setImageResource(R.drawable.tongue);

        if(emoticonName.equals("Crying"))
        	holder.imageView.setImageResource(R.drawable.cry);

        if(emoticonName.equals("Happy"))
        	holder.imageView.setImageResource(R.drawable.grin);

        if(emoticonName.equals("Sad"))
        	holder.imageView.setImageResource(R.drawable.sad);

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