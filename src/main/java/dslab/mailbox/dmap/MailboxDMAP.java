package dslab.mailbox.dmap;

import dslab.util.Config;
import dslab.util.Email;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MailboxDMAP implements Runnable{

    private Socket socket;
    private ConcurrentHashMap<Integer, Email> emails;
    private Config config;
    private Config userConfig;
    private boolean login = false;
    private boolean quit = false;
    private PrintWriter writer;
    private String loggedUser=null;
    private ConcurrentLinkedQueue<Socket> sockets = new ConcurrentLinkedQueue<>();

    public MailboxDMAP(Socket socket, ConcurrentHashMap<Integer, Email> emails, Config config, ConcurrentLinkedQueue<Socket> sockets) {
        this.config = config;
        this.socket = socket;
        this.sockets = sockets;
        this.emails = emails;
        this.userConfig = new Config(this.config.getString("users.config"));
    }

    @Override
    public void run() {
        try {
            System.out.println("running DMAP thread: " + Thread.currentThread());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMAP");
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

                if(Objects.equals(response, "ok bye") || Objects.equals(response, "error on protocol")){
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

    /**
     * Handles the request according to the DMAP Protocoll.
     * @param requests an array of commands and arguments
     * @return appropriate response
     */
    private String handleRequest(String[] requests){
        if(requests.length == 0){
            quit = true;
            return "invalid request";
        }
        if(Objects.equals(requests[0], "login")){
            return login(requests);
        }

        if(Objects.equals(requests[0], "list")){
            return list(requests);
        }

        if(Objects.equals(requests[0], "show")){
            return show(requests);
        }

        if(Objects.equals(requests[0], "delete")){
            return delete(requests);
        }

        if(Objects.equals(requests[0], "logout")){
            return logout(requests);
        }

        if(Objects.equals(requests[0], "quit")){
            quit = true;
            return "ok bye";
        }

        quit = true;
        return "error on protocol";
    }

    /**
     * Handles the login command.
     * @param requests an array of commands and arguments (first element of the array is "login")
     * @return appropriate response
     */
    private String login(String[] requests){
        if(login){
            return "already logined in";
        }
        if(requests.length != 3){
            return "login <username> <password>";
        }
        else{
            String username = requests[1];
            String password = requests[2];

            try {
                if(!Objects.equals(userConfig.getString(username), password)){
                    return "error wrong password";
                }
            } catch (MissingResourceException e){
                return "error no such user";
            }

            loggedUser = requests[1];
            login = true;
            return "ok";
        }
    }

    /**
     * Handles the list command.
     * @param requests an array of commands (first element of the array is "list")
     * @return appropriate response
     */
    private String list(String[] requests){
        if(!login){
            return "error not logged in";
        }
        if(requests.length != 1){
            return "error list does not take any arguments";
        }
        else{
            String response = "";
            LinkedList<Email> userMails = listMails(loggedUser);

            int counter = 1;
            boolean foundMail = false;
            for(Email e : userMails){
                if(!e.deleted(loggedUser, config.getString("domain"))){
                    response = response + counter + " " + e.getFromAdress() + " " + e.getSubject() + "\n";
                    foundMail = true;
                }
                counter++;
            }

            if(userMails.size() == 0 || !foundMail){
                return "no mails";
            }

            return response.substring(0, response.length()-1);
        }
    }

    /**
     * Handles the show command.
     * @param requests an array of commands and arguments (first element of the array is "show")
     * @return appropriate response
     */
    private String show(String[] requests){
        if(!login){
            return "not logged in";
        }
        if(requests.length != 2){
            return "error show <id>";
        }
        else{
            String response = "!";
            LinkedList<Email> userMails = listMails(loggedUser);

            int counter = 1;
            for(Email e : userMails){
                if(String.valueOf(counter).equals(requests[1]) && !e.deleted(loggedUser, config.getString("domain"))){
                    response =  "from: " + e.getFromAdress() + "\n" +
                            "to: " + e.listRecipients() + "\n";

                    if(e.getSubject() != null){
                        response = response + "subject: " + e.getSubject() + "\n";
                    }
                    else {
                        response = response + "subject: " + "\n";
                    }

                    if(e.getData() != null){
                        response = response + "data: " + e.getData() + "\n";
                    }
                    else{
                        response = response + "data: \n";
                    }

                }
                counter++;
            }
            if(response.equals("!")){
                return "error no email with id: " + requests[1];
            }
            return response.substring(0, response.length()-1);
        }
    }

    /**
     * Handles the delete command.
     * @param requests an array of commands and arguments (first element of the array is "delete")
     * @return appropriate response
     */
    private String delete(String[] requests){
        if(!login){
            return "not logged in";
        }
        if(requests.length != 2){
            return "error delete <id>";
        }
        else{
            LinkedList<Email> userMails = listMails(loggedUser);
            String response = "error";
            int counter = 1;
            for(Email e : userMails){
                if(String.valueOf(counter).equals(requests[1])){
                    e.deleteMail(loggedUser, config.getString("domain"));
                    response = "ok";
                    break;
                }
                counter++;
            }
            return response;
        }
    }

    private String logout(String[] requests){
        if(!login){
            return "not logged in";
        }
        if(requests.length != 1){
            return "error logout does not take any arguments";
        }
        else{
            login = false;
            return "ok";
        }
    }

    private LinkedList<Email> listMails(String username){
        Collection<Email> emailValues = emails.values();
        LinkedList<Email> userMails = new LinkedList<>();
        for(Email e : emailValues){
            LinkedList<String> toAdresses = e.getToAdress();
            for(String item : toAdresses){
                if(item.contains(username)){
                    userMails.add(e);
                }
            }
        }

        return userMails;
    }
}
