package dslab.util;

import java.util.LinkedList;

public class Email {

    private int id;
    private LinkedList<String> toAdress = new LinkedList<>();
    private String fromAdress;
    private String subject;
    private String data;

    private LinkedList<String> deleted = new LinkedList<>();

    @Override
    public String toString() {
        return "Email{" +
                "id=" + id +
                ", toAdress=" + toAdress.toString() +
                ", fromAdress='" + fromAdress + '\'' +
                ", subject='" + subject + '\'' +
                ", data='" + data + '\'' +
                ", deleted='" + deleted + '\'' +
                '}';
    }

    public Email(int id, LinkedList<String> toAdress, String fromAdress, String subject, String data) {
        this.id = id;
        this.toAdress = toAdress;
        this.fromAdress = fromAdress;
        this.subject = subject;
        this.data = data;
    }

    public Email(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public LinkedList<String> getToAdress() {
        return toAdress;
    }

    public String toAdresse() {
        String response = "";
        for(String str : toAdress){
            response = response + str + ",";
        }
        return response.substring(0, response.length()-1);
    }

    public String getFromAdress() {
        return fromAdress;
    }

    public String getSubject() {
        return subject;
    }

    public String getData() {
        return data;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setToAdress(LinkedList<String> toAdress) {
        this.toAdress = toAdress;
    }

    public void setFromAdress(String fromAdress) {
        this.fromAdress = fromAdress;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String listRecipients(){
        String response = "";
        for(String s : toAdress){
            response = response + s + ",";
        }
        return response.substring(0, response.length()-1);
    }

    public void deleteMail(String username, String domain){
        deleted.add(username + "@" + domain);
    }

    public boolean deleted(String username, String domain){
        /*System.out.println(username+"@"+domain);
        System.out.println(deleted.contains(username+"@"+domain));*/
        return deleted.contains(username+"@"+domain);
    }
}
