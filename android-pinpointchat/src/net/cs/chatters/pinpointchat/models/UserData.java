package net.cs.chatters.pinpointchat.models;

import java.io.Serializable;

public class UserData implements Serializable{

	private static final long serialVersionUID = 1L;
	private String name;
    private int distanceFromUser;
    private String regid;
    public  double   lat;
    public  double 	lng;

    public String getRegid(){
        return regid;
    }

    public void setRegid(String regid){
        this.regid = regid;
    }

    public UserData(String username){
        name = username;
    }

    public UserData(){
        ;
    }

    public String getName() {
        return name;
    }

    public int getDistanceFromUser() {
        return distanceFromUser;
    }

    public void setName(String str) {
        name = str;
    }

    public void setDistanceFromUser(int str) {
        distanceFromUser = str;
    }
    
}
