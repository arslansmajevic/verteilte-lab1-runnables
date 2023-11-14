package dslab.mailbox.dmap;

import dslab.util.Config;
import dslab.util.Email;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadDMAP implements Runnable{

    private Config config;
    private ServerSocket serverSocket;
    private ExecutorService pool = Executors.newFixedThreadPool(20);
    private ConcurrentHashMap<Integer, Email> emails;
    private ConcurrentLinkedQueue<Socket> sockets = new ConcurrentLinkedQueue<>();

    public ListenerThreadDMAP(ServerSocket serverSocket, Config config, ConcurrentHashMap<Integer, Email> emails) {
        this.config=config;
        this.serverSocket=serverSocket;
        this.emails=emails;
    }

    @Override
    public void run() {
        while(true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                sockets.add(socket);
                pool.execute(new MailboxDMAP(socket,emails,config,sockets));
            } catch (IOException e) {
                System.out.println("serversocket was closed, shutting down poolthread on dmap...");
                pool.shutdown();
                // pool.shutdownNow();
                break;
            }
        }
    }
}
