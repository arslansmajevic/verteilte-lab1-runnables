package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private String componentId;
    private Config config;
    private InputStream in;
    private OutputStream out;
    private Shell shell;
    private DatagramSocket datagramSocket;
    private Thread monitoringThread;
    private ConcurrentHashMap<String, Integer> addresses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> servers = new ConcurrentHashMap<>();

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId; // monitoring
        this.config = config;
        this.in = in;
        this.out = out;
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + ".server> ");

        System.out.println(componentId + ": {udp.port: " + this.config.getInt("udp.port") + "}");
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));
        } catch (SocketException e) {
            e.printStackTrace();
            this.shutdown();
        }

        monitoringThread = new Thread(new MonitoringThread(datagramSocket, addresses, servers));
        monitoringThread.start();

        shell.run();
    }

    @Override
    @Command
    public void addresses() {
        for (ConcurrentHashMap.Entry<String, Integer> adr : addresses.entrySet()) {
            shell.out().println(adr.getKey() + " " + adr.getValue());
        }
    }

    @Override
    @Command
    public void servers() {
        for (ConcurrentHashMap.Entry<String, Integer> serv : servers.entrySet()) {
            shell.out().println(serv.getKey() + " " + serv.getValue());
        }
    }

    @Override
    @Command
    public void shutdown() {
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
