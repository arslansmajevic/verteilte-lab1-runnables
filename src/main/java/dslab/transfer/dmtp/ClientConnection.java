package dslab.transfer.dmtp;

import dslab.util.Config;
import dslab.util.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class ClientConnection implements Runnable{

    private Config domainsConfig = new Config("domains.properties");
    private Socket mailboxSocket;
    private PrintWriter writer;
    private Socket socket;
    private boolean quit = false;
    private Email email;
    private Config config;
    private boolean begin = false;

    public ClientConnection(Socket socket, Config config, int emailId) {
        this.socket = socket;
        this.email = new Email(emailId);
        this.config = config; // transfer-1 for Example
    }

    @Override
    public void run() {
        try {
            System.out.println("running DMTP thread: " + Thread.currentThread());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMTP");
            writer.flush();

            while (!quit) {
                String request = reader.readLine();
                System.out.println(Thread.currentThread() + ": reporting request: [" + request + "], on socket: " + socket);

                if(request == null){
                    socket.close();
                    quit = true;
                    break;
                }

                String[] requests = request.split("\\s");
                String response = handleRequest(requests);
                writer.println(response);
                writer.flush();

                if (response.equals("ok bye") || response.equals("error protocol")) {
                    socket.close();
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage() + " on this thread " + Thread.currentThread());

        }
        finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("error"+e.getMessage());
                }
            }
        }
    }

    private String handleRequest(String[] requests){
        if(requests.length == 0){
            return "error protocol";
        }
        if(Objects.equals(requests[0], "begin")){
            begin = true;
            return "ok";
        }

        if(begin){
            if(Objects.equals(requests[0], "subject")){
                String mergedString = String.join(" ", Arrays.copyOfRange(requests, 1, requests.length));
                this.email.setSubject(mergedString);
                return "ok";
            }

            if(Objects.equals(requests[0], "to")){
                if(requests.length != 2){
                    return "from <adress>[,<adresses>...]";
                }
                else {
                    String[] adresses = requests[1].split(",");
                    LinkedList<String> temp = new LinkedList<>(Arrays.asList(adresses));

                    String response = checkRecipients(temp);

                    //System.out.println(temp.toString());
                    if(response.startsWith("ok")){
                        this.email.setToAdress(temp);
                        return response;
                    }
                    else{
                        return response;
                    }
                }
            }

            if(Objects.equals(requests[0], "from")){
                if(requests.length != 2){
                    return "from <adress>";
                }
                else {
                    String checkAdress = checkFromAdress(requests[1]);
                    if(checkAdress.equals("ok")){
                        this.email.setFromAdress(requests[1]);
                    }
                    return checkAdress;
                }
            }

            if(Objects.equals(requests[0], "data")){
                String mergedString = String.join(" ", Arrays.copyOfRange(requests, 1, requests.length));
                this.email.setData(mergedString);
                return "ok";
            }

            if(Objects.equals(requests[0], "send")){

                if(this.email.getToAdress() == null || this.email.getToAdress().size() == 0){
                    return "error no recipient name(s)";
                }

                if(!checkRecipients(this.email.getToAdress()).contains("ok")){
                    return "error no recipients";
                }

                if(this.email.getFromAdress() == null || this.email.getFromAdress() == ""){
                    return "error no sender name";
                }
                startTransfer();

                return "ok";
            }

            if(Objects.equals(requests[0], "quit")){
                return "ok bye";
            }
        }

        return "error protocol";
    }

    private String checkRecipients(LinkedList<String> recipients){
        for(String mail : recipients){
            if(!mail.contains("@")){
                return "error invalid recipient " + mail;
            }
            else{
                String[] splitMail = mail.split("@");
                if(splitMail.length != 2){
                    return "error invalid recipient " + mail;
                }
            }
        }

        return "ok " + recipients.size();
    }

    private String checkFromAdress(String fromAdress){
        if(!fromAdress.contains("@")){
            return "error invalid sender adress: " + fromAdress;
        }

        String[] split = fromAdress.split("@");

        if(split.length != 2){
            return "error invalid sender adress: " + fromAdress;
        }

        if(!split[1].contains(".")){
            return "error invalid sender adress: " + fromAdress;
        }

        return "ok";
    }

    private void startTransfer() {
        System.out.println("starting transfer");
        LinkedList<String> toAdresses = email.getToAdress();
        HashMap<String, Integer> sentDomains = new HashMap<>();

        for (String adress : toAdresses) {
            String[] adressData = adress.split("@");
            if (!sentDomains.containsKey(adressData[1])) {
                sentDomains.put(adressData[1], 1);
            }
            if (domainLookup(adressData[1])) {
                if (sentDomains.get(adressData[1]) == 1) {
                    String temp = sendMail(adressData[1]);
                    if(temp.equals("domain not available")){
                        remailOnTheSender(email.getFromAdress());
                    }
                    sentDomains.put(adressData[1], 2);
                }
            } else {
                remailOnTheSender(email.getFromAdress());
            }
        }
    }

    private void remailOnTheSender(String from) {
        String[] splitFrom = from.split("@");
        LinkedList<String> tempToAdress = new LinkedList<>();
        tempToAdress.add(from);
        Email remailingMail = new Email(email.getId(), tempToAdress, "mailer@"+splitFrom[1], email.getSubject(), email.getData());

        this.email = remailingMail;

        String message = sendMail(splitFrom[1]);
    }

    private String sendMail(String domain){
        String[] hostAndPort = new String[0];

        try{
            hostAndPort = domainsConfig.getString(domain).split(":");
        } catch (MissingResourceException m){
            return "error no domain with such name";
        }

        try{
            System.out.println(hostAndPort[0] + " " + hostAndPort[1]);

            mailboxSocket = new Socket(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
            PrintWriter writer = new PrintWriter(mailboxSocket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(mailboxSocket.getInputStream()));

            String message = "";

            reader.readLine();
            writer.write("begin\r\n");
            writer.flush();

            System.out.println(reader.readLine());

            writer.write("to " + email.toAdresse() + "\r\n");
            writer.flush();
            message = reader.readLine();
            System.out.println(message);

            if(message.contains("erorr")){
                // cannot happen essentially
            }

            writer.write("subject " + email.getSubject() + "\r\n");
            writer.flush();
            System.out.println(reader.readLine());

            writer.write("from " + email.getFromAdress() + "\r\n");
            writer.flush();
            System.out.println(reader.readLine());

            writer.write("data " + email.getData() + "\r\n");
            writer.flush();
            System.out.println(reader.readLine());

            writer.write("send" + "\r\n");
            writer.flush();
            System.out.println(reader.readLine());


            writer.write("quit" + "\r\n");
            writer.flush();

            mailboxSocket.close();

        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage() + ", socket closed");
            return "domain not available";
        }

        sendDataToMonitoring(this.email.getFromAdress());
        return "ok";
    }

    private void sendDataToMonitoring(String fromAdress) {
        LinkedList<String> toAdresses = email.getToAdress();
        int destinationPort = config.getInt("monitoring.port");

        for(String adress : toAdresses){
            String domain = adress.split("@")[1];
            if(domainsConfig.containsKey(domain)){ // mail can be sent to statistics
                try {
                    String message = domainsConfig.getString(domain) + " " + this.email.getFromAdress();
                    // ip adress:port + mail adress

                    DatagramSocket datagramSocket = new DatagramSocket();
                    byte[] buffer = message.getBytes();

                    String ipAdress = domainsConfig.getString(domain);
                    String split[] = ipAdress.split(":");
                    ipAdress = split[0];

                    InetAddress addressForSocket = InetAddress.getByName(ipAdress);

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addressForSocket, destinationPort);
                    datagramSocket.send(packet);

                    System.out.println("UDP message sent to " + addressForSocket.getHostAddress() + " on port " + destinationPort + ", with content: " + message);

                    datagramSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    String ipAdress = domainsConfig.getString(domain);
                    String split[] = ipAdress.split(":");
                    ipAdress = split[0]; // 127.0.0.1

                    String message = ipAdress + ":" + config.getString("tcp.port");
                    // ip adress:port

                    DatagramSocket datagramSocket = new DatagramSocket();
                    byte[] buffer = message.getBytes();

                    InetAddress addressForSocket = InetAddress.getByName(ipAdress);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addressForSocket, destinationPort);

                    datagramSocket.send(packet);

                    System.out.println("UDP message sent to " + addressForSocket.getHostAddress() + " on port " + destinationPort + ", with content: " + message);

                    datagramSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean domainLookup(String domain) {
        if(domainsConfig.containsKey(domain))
            return true;
        else
            return false;
    }
}
