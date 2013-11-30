package net.cs.chatters.pinpointchat.models;

/**
 * Created by arthur on 7/18/13.
 */
public class Message implements Comparable{

    private String content;
    private String sender;
    private String receiver;
    private long date;          //messages are fetched by date
    private String hourMin;     //it is displayed by the username
    public int msgNo;
    private boolean sent;


    public Message() {
    }

    public Message(String sender, String receiver, String content, boolean sent, int msgNo) {

        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.msgNo = msgNo;
        this.sent = sent;

        date = System.currentTimeMillis();
    }

    public void setHourMin(String hourMin){
        this.hourMin = hourMin;
    }

    public String getHourMin(){
        return this.hourMin;
    }

    public void setContent(String _content) {
        content = _content;
    }

    public void setSender(String _sender) {
        sender = _sender;
    }

    public void setReceiver(String _receiver) {
        receiver = _receiver;
    }

    public void setDate(long _date) {
        date = _date;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public long getDate() {
        return date;
    }

    @Override
    public int compareTo(Object o) {

        return this.msgNo - ((Message)o).msgNo;
    }
}
