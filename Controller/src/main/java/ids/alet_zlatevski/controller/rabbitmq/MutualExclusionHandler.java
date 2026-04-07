package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.BROADCAST_EXCHANGE;

public class MutualExclusionHandler {

    protected final int nodeNumber;

    protected final String brokerHostname;
    protected final Connection connection;
    protected final Channel channel;

    protected final ArrayList<String> nodeIds;
    protected final String nodeId;

    protected final String broadcastChannel;
    protected final Semaphore handshakeSemaphore;
    protected final Semaphore lockSemaphore;
    protected boolean asking;
    protected int LC;
    protected final int LD;
    protected int nb_perm;
    protected final Map<Integer, Boolean> delayed;


    public MutualExclusionHandler(final String brokerHostname, int nodeNumber, String nodeId, Semaphore handshakeSemaphore, Semaphore lockSemaphore) throws IOException, TimeoutException, InterruptedException {
        this.brokerHostname = brokerHostname;
        this.nodeNumber = nodeNumber;
        this.nodeIds = new ArrayList<>();
        this.nodeId = nodeId;
        this.broadcastChannel = getBroadcastChannelForId(this.nodeId);
        this.handshakeSemaphore =  handshakeSemaphore;
        this.lockSemaphore = lockSemaphore;
        this.asking = false;
        this.LC = 0;
        this.LD = -1;
        this.nb_perm = 0;
        this.delayed = new HashMap<>();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerHostname);
        this.connection = factory.newConnection();

        this.channel = connection.createChannel();
        this.channel.exchangeDeclare(BROADCAST_EXCHANGE, "fanout");

        this.channel.queueDeclare(this.broadcastChannel, false, true, true, null);
        this.channel.queueBind(this.broadcastChannel, BROADCAST_EXCHANGE, "");

        this.channel.queueDeclare(nodeId, false, true, true, null);

        this.setupHandshakeCallback();
        this.setupMutualExclusionCallback();
        this.initiateHandshakeProcedure();
    }


    protected void setupHandshakeCallback() throws IOException {
        this.channel.basicConsume(this.broadcastChannel, new BroadcastDispatcher(this));
    }

    protected void initiateHandshakeProcedure() throws IOException, InterruptedException {
        Packet packet = new Packet(PacketType.HANDSHAKE);
        packet.setData(nodeId);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
        this.handshakeSemaphore.acquire();
    }

    protected void handshakeFinished() {
        System.out.println("ALL NODE ARE CONNECTED TO EACH OTHERS");
        this.handshakeSemaphore.release();
    }

    protected void setupMutualExclusionCallback() throws IOException {
        this.channel.basicConsume(this.nodeId,  new LockDispatcher(this));
    }

    public void initiateWantLock() throws IOException, InterruptedException {
        Packet packet = new Packet(PacketType.LOCK);
        packet.setData(nodeId + "," + LD);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());

        this.asking = true;
        this.nb_perm = 0;

        this.lockSemaphore.acquire();
    }

    protected void lockGranted() {
        System.out.println("Node " + nodeId + " has the lock!");
        this.lockSemaphore.release();
    }

    public void initiateUnlock() throws IOException { // rule 6
        for(Integer IDs: delayed.keySet()) {
            if(delayed.get(IDs)) {
                Packet ongoing = new Packet(PacketType.ACK);
                ongoing.setData(nodeId + "," + LC);
                this.channel.basicPublish("", String.valueOf(IDs), null, ongoing.toByteArray());
                delayed.replace(IDs, false);
            }
        }
        asking = false;
        nb_perm = 0;
    }

    public String getNodeId() {
        return nodeId;
    }

    public static String getBroadcastChannelForId(String nodeId) {
        return "BROADCAST_" + nodeId;
    }
}
