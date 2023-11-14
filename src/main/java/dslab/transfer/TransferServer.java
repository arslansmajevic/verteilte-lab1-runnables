package dslab.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.dmap.ListenerThreadDMAP;
import dslab.mailbox.dmtp.ListenerThreadDMTP;
import dslab.transfer.dmtp.ListenerThreadTransfer;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

    private Shell shell;
    private ServerSocket serverSocketDMTP;
    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private Thread listenerThreadTransfer;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + ".server> ");

        System.out.println(componentId + ": {dmtp.tcp.port: " + this.config.getInt("tcp.port") + "}");
    }

    @Override
    public void run() {
        try {
            this.serverSocketDMTP = new ServerSocket(config.getInt("tcp.port"));

            listenerThreadTransfer = new Thread(new ListenerThreadTransfer(serverSocketDMTP, config));
            listenerThreadTransfer.start();
            shell.run();
        } catch (IOException e) {
            System.out.println("IOException"+e.getMessage());
        }
    }

    @Override
    @Command
    public void shutdown() {

        try {
            serverSocketDMTP.close();
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
