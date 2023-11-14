package dslab.mailbox.dmtp;

import dslab.util.Config;
import dslab.util.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxDMTP implements Runnable{

    private Socket socket;
    private boolean begin = false;
    private Email email;
    private ConcurrentHashMap<Integer, Email> mails;
    private Config config;
    private PrintWriter writer;
    private boolean quit = false;

    public MailboxDMTP(Socket socket, Config config, ConcurrentHashMap<Integer, Email> mails, int emailId) {
        this.socket = socket;
        this.mails = mails;
        this.config = config;
        this.email = new Email(emailId);
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

                String[] requests = request.split(" ");
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

    private String handleRequest(String[] requests) {
        if (requests.length == 0) {
            return "error protocol";
        }
        if (Objects.equals(requests[0], "begin")) {
            begin = true;
            return "ok";
        }

        if (begin) {
            if (Objects.equals(requests[0], "subject")) {
                String mergedString = String.join(" ", Arrays.copyOfRange(requests, 1, requests.length));
                this.email.setSubject(mergedString);
                return "ok";
            }

            if (Objects.equals(requests[0], "to")) {
                if (requests.length != 2) {
                    return "from <adress>[,<adresses>...]";
                } else {
                    String[] adresses = requests[1].split(",");
                    LinkedList<String> temp = new LinkedList<>(Arrays.asList(adresses));

                    String response = checkRecipients(temp);

                    if (response.startsWith("ok")) {
                        this.email.setToAdress(temp);
                        return response;
                    }
                    if(response.startsWith("error unknown recipient")){
                        return response;
                    }
                    return response;
                }
            }

            if (Objects.equals(requests[0], "from")) {
                if (requests.length != 2) {
                    return "from <adress>";
                } else {
                    String checkAdress = checkFromAdress(requests[1]);
                    if (checkAdress.equals("ok")) {
                        this.email.setFromAdress(requests[1]);
                    }
                    return checkAdress;
                }
            }

            if (Objects.equals(requests[0], "data")) {
                String mergedString = String.join(" ", Arrays.copyOfRange(requests, 1, requests.length));
                this.email.setData(mergedString);
                return "ok";
            }

            if (Objects.equals(requests[0], "send")) {

                if (this.email.getToAdress() == null || this.email.getToAdress().size() == 0) {
                    return "error no recipient name(s)";
                }

                if (!checkRecipients(this.email.getToAdress()).contains("ok")) {
                    return "error unknown recipients";
                }

                if (this.email.getFromAdress() == null || this.email.getFromAdress() == "") {
                    return "error no sender name";
                }

                System.out.println(email.toString());
                mails.put(email.getId(), email);
                return "ok";
            }

            if (Objects.equals(requests[0], "quit")) {
                return "ok bye";
            }
        }

        return "error protocol";
    }

    private String checkRecipients(LinkedList<String> recipients) {

        int okMails = recipients.size();
        for (String mail : recipients) { // iterating mails
            if (!mail.contains("@")) { // @ not present
                return "error unknown recipient " + mail;
            } else {
                String[] splitMail = mail.split("@"); // splitting on @
                if (splitMail.length != 2) {
                    return "error unknown recipient " + mail;
                } else {
                    if (!Objects.equals(splitMail[1], config.getString("domain"))) { // unknown domain
                        if (checkDomain(splitMail)) { // returns true if a domain is inside the accepted domains
                            okMails--;

                        } else {
                            return "error unknown recipient " + mail;
                        }
                    }
                }

                if(!isUserOfDomain(splitMail))
                {
                    okMails--;
                    return "error unknown recipient " + mail;
                }
            }
        }

        return "ok " + okMails;
    }

    private boolean isUserOfDomain(String[] splitMail) {

        String usersConfigFile = "users-" + splitMail[1].replace('.', '-');
        Config users = new Config(usersConfigFile);

        if(users.containsKey(splitMail[0]))
            return true;
        else
            return false;
    }

    private String checkFromAdress(String fromAdress) {
        if (!fromAdress.contains("@")) {
            return "error invalid sender adress: " + fromAdress;
        }

        String[] split = fromAdress.split("@");

        if (split.length != 2) {
            return "error invalid sender adress: " + fromAdress;
        }

        if (!split[1].contains(".")) {
            return "error invalid sender adress: " + fromAdress;
        }

        return "ok";
    }

    private boolean checkDomain(String[] toAdress) {
        Config domainsConfig = new Config("domains");

        Set<String> domains = domainsConfig.listKeys();

        for (String domain : domains) {
            if (domain.equals(toAdress[1])) {
                String tempDomain = domain.replace('.', '-');
                Config mailboxConfig = new Config("mailbox-" + tempDomain);
                Config users = new Config(mailboxConfig.getString("users.config"));
                if (users.containsKey(toAdress[0])) {
                    return true;
                }
            }
        }

        return false;
    }

}
