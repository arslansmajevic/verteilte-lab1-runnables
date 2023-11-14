package dslab.mailbox.dmtp;

import dslab.mailbox.dmap.MailboxDMAP;
import dslab.util.Config;
import dslab.util.Email;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadDMTP implements Runnable{

    private ServerSocket serverSocket;
    private ConcurrentHashMap<Integer, Email> emails;
    private Config config;
    private ExecutorService pool = Executors.newFixedThreadPool(20);
    private int emailId = 1;

    public ListenerThreadDMTP(ServerSocket serverSocket, Config config, ConcurrentHashMap<Integer, Email> mails) {
        this.serverSocket = serverSocket;
        this.config = config;
        this.emails = mails;

    }

    @Override
    public void run() {
        while(true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                pool.execute(new MailboxDMTP(socket,config, emails, emailId++));
            } catch (IOException e) {
                System.out.println("serversocket was closed, shutting down poolthread on dmtp...");
                pool.shutdown();
                // pool.shutdownNow();
                break;
            }
        }
    }
}
