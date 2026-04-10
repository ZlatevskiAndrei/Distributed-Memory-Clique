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

    protected final String brokerHostname;
    protected final Connection connection;
    protected final Channel channel;

    protected final ArrayList<String> nodeIds;
    protected final String nodeId;

    protected final Semaphore concurrentOperationSemaphore;
    protected final Semaphore acquireLockSemaphore;
    protected boolean asking;
    protected int LC;
    protected final int LD;
    protected int nb_perm;
    protected final Map<Integer, Boolean> delayed;


    public MutualExclusionHandler(final String brokerHostname, String nodeId) throws IOException, TimeoutException, InterruptedException {
        this.brokerHostname = brokerHostname;
        this.nodeIds = new ArrayList<>();
        this.nodeId = nodeId;
        this.concurrentOperationSemaphore = new Semaphore(0); // for now we can't execute any operation. Finishing handshake will allow one.
        this.acquireLockSemaphore = new Semaphore(0);
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

        this.channel.queueDeclare(this.nodeId, false, true, true, null);
        this.channel.queueBind(this.nodeId, BROADCAST_EXCHANGE, "");

        this.setupCallback();
        this.initiateHandshakeProcedure();
    }


    protected void setupCallback() throws IOException {
        this.channel.basicConsume(this.nodeId, new PacketDispatcher(this));
    }

    protected void initiateHandshakeProcedure() throws IOException {
        Packet packet = new Packet(PacketType.HANDSHAKE);
        packet.setData(nodeId);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
    }

    protected void handshakeFinished() {
        System.out.println("ALL NODE ARE CONNECTED TO EACH OTHERS.");
        // We allow one lock at time
        this.concurrentOperationSemaphore.release(1);
    }

    public void initiateWantLock() throws IOException, InterruptedException {
        this.concurrentOperationSemaphore.acquire();

        Packet packet = new Packet(PacketType.LOCK);
        packet.setData(nodeId + "," + LD);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());

        this.asking = true;
        this.nb_perm = 0;

        this.acquireLockSemaphore.acquire();
    }

    protected void lockGranted() {
        System.out.println("Node " + nodeId + " has the lock!");
        this.acquireLockSemaphore.release();
    }

    public void initiateUnlock() throws IOException { // rule 6
        for (Integer IDs : this.delayed.keySet()) {
            if (this.delayed.get(IDs)) {
                Packet ongoing = new Packet(PacketType.ACK);
                ongoing.setData(this.nodeId + "," + this.LC);
                this.channel.basicPublish("", String.valueOf(IDs), null, ongoing.toByteArray());
                this.delayed.replace(IDs, false);
            }
        }
        this.asking = false;
        this.nb_perm = 0;
        this.concurrentOperationSemaphore.release();
    }

    public String getNodeId() {
        return nodeId;
    }

}
