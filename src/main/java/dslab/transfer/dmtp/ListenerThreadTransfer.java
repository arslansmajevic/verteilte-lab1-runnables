package dslab.transfer.dmtp;

import dslab.mailbox.dmtp.MailboxDMTP;
import dslab.util.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadTransfer implements Runnable{

    private ServerSocket serverSocket;
    private Config config;
    private ExecutorService pool = Executors.newFixedThreadPool(2);
    private int emailId = 1;

    public ListenerThreadTransfer(ServerSocket serverSocket, Config config) {
        this.serverSocket = serverSocket;
        this.config = config;
    }

    @Override
    public void run() {
        while(true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                pool.execute(new ClientConnection(socket, config, emailId++));
            } catch (IOException e) {
                System.out.println("serversocket was closed, shutting down poolthread on dmtp...");
                pool.shutdown();
                // pool.shutdownNow();
                break;
            }
        }
    }
}
