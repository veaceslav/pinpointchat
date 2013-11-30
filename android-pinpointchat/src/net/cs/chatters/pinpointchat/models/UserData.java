package net.cs.chatters.pinpointchat.models;

public class UserData {
    private String name;
    private int distanceFromUser;
    private String regid;

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
