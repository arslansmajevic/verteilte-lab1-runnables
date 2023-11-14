package dslab.monitoring;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringThread implements Runnable{

    private DatagramSocket datagramSocket;
    private ConcurrentHashMap<String, Integer> addresses;
    private ConcurrentHashMap<String, Integer> servers;

    public MonitoringThread(DatagramSocket socket, ConcurrentHashMap<String, Integer> adresses, ConcurrentHashMap<String, Integer> servers) {
        this.datagramSocket = socket;
        this.addresses = adresses;
        this.servers = servers;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet;
        while(true){
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] split = received.split("\\s");

                System.out.println("received packet: " + received);

                if(split.length == 1){
                    servers.merge(split[0], 1, Integer::sum);
                }

                if(split.length == 2){
                    servers.merge(split[0], 1, Integer::sum);
                    addresses.merge(split[1], 1, Integer::sum);
                }

            } catch (IOException e) {
                System.out.println("datagramSocket was closed...");
                break;
            }
        }
    }
}
