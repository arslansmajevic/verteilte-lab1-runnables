package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.dmap.ListenerThreadDMAP;
import dslab.mailbox.dmtp.ListenerThreadDMTP;
import dslab.util.Config;
import dslab.util.Email;

public class MailboxServer implements IMailboxServer, Runnable {

    private ServerSocket serverSocketDMAP;
    private ServerSocket serverSocketDMTP;
    private Shell shell;
    private ConcurrentHashMap<Integer, Email> mails = new ConcurrentHashMap<>();
    private String componentId;
    private Config config;

    private Thread listenerThreadDMAP;
    private Thread listenerThreadDMTP;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.componentId = componentId;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + ".server> ");

        System.out.println("{domain: " + this.config.getString("domain") +
                ", dmap.tcp.port: " + this.config.getInt("dmap.tcp.port") +
                ", dmtp.tcp.port: " + this.config.getInt("dmtp.tcp.port") + "}");
    }

    @Override
    public void run() {

        try {
            this.serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
            this.serverSocketDMTP = new ServerSocket(config.getInt("dmtp.tcp.port"));

            listenerThreadDMAP = new Thread(new ListenerThreadDMAP(serverSocketDMAP, config, mails));
            listenerThreadDMAP.start();

            listenerThreadDMTP = new Thread(new ListenerThreadDMTP(serverSocketDMTP, config, mails));
            listenerThreadDMTP.start();
            shell.run();
        } catch (IOException e) {
            System.out.println("IOException"+e.getMessage());
        }
    }

    @Override
    @Command
    public void shutdown() {

        try {
            serverSocketDMAP.close();
            serverSocketDMTP.close();
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
