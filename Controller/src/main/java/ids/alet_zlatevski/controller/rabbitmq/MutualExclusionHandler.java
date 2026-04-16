package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ids.alet_zlatevski.controller.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.BROADCAST_EXCHANGE;
import static ids.alet_zlatevski.env.Constants.NODE_NUMBER;

public class MutualExclusionHandler {

    protected final String brokerHostname;
    protected final Connection connection;
    protected final Channel channel;

    protected final ArrayList<String> nodeIds;
    protected final String nodeId;

    protected final Semaphore concurrentOperationSemaphore;
    protected final Semaphore acquireLockSemaphore;
    protected final Semaphore globalUpdateSemaphore;
    protected boolean asking;
    protected int LC;
    protected int LD;
    protected int nb_perm;
    protected int global_update_acks;
    protected final Map<Integer, Boolean> delayed;
    protected final Controller controller; //tightly coupled anyway


    public MutualExclusionHandler(final String brokerHostname, String nodeId, Controller controller) throws IOException, TimeoutException, InterruptedException {
        this.brokerHostname = brokerHostname;
        this.nodeIds = new ArrayList<>();
        this.nodeId = nodeId;
        this.concurrentOperationSemaphore = new Semaphore(0); // for now we can't execute any operation. Finishing handshake will allow one.
        this.acquireLockSemaphore = new Semaphore(0);
        this.globalUpdateSemaphore = new Semaphore(0);
        this.asking = false;
        this.LC = 0;
        this.LD = -1;
        this.nb_perm = 0;
        this.global_update_acks = 0;
        this.delayed = new HashMap<>();
        this.controller = controller;

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
        if (NODE_NUMBER <= 1) {
            this.handshakeFinished();
            return;
        }
        Packet packet = new Packet(PacketType.HANDSHAKE);
        packet.setData(nodeId);
        System.out.println("Initiating handshake for ID " + this.nodeId);
        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
    }

    public void initiateGlobalUpdateProcedure(int addr, long val) throws InterruptedException, IOException {
        if (NODE_NUMBER <= 1) {
            this.globalUpdateFinished();
            return;
        }
        Packet packet = new Packet(PacketType.GLOBAL_UPDATE);
        String data = this.nodeId + "," + addr + "," + val;
        packet.setData(data);
        this.global_update_acks = 0;
        System.out.println("Initiating update procedure for " + this.nodeId);
        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
        this.globalUpdateSemaphore.acquire();
    }

    protected void distantMemoryUpdate(int addr, long val) throws InterruptedException {
        System.out.println("Received distant memory update for addr " + addr + " and val " + val);
        this.controller.updateBroadcastChanges(addr, val);
    }

    protected void globalUpdateFinished() {
        System.out.println("Releasing global update semaphore for ID " + this.nodeId);
        this.global_update_acks = 0;
        this.globalUpdateSemaphore.release();
    }


    protected void handshakeFinished() {
        System.out.println("ALL NODES ARE CONNECTED TO EACH OTHER.");
        // We allow one lock at time
        this.concurrentOperationSemaphore.release(1);
    }

    public void initiateWantLock() throws IOException, InterruptedException {
        this.concurrentOperationSemaphore.acquire();

        System.out.println("Initiating Want LOCK for " + this.nodeId);

        if (NODE_NUMBER <= 1 || this.nodeIds.isEmpty()) {
            this.lockGranted();
            return;
        }

        this.LC += 1;
        this.LD = this.LC;
        this.asking = true;
        this.nb_perm = 0;

        Packet packet = new Packet(PacketType.LOCK);
        packet.setData(nodeId, LD);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
        this.acquireLockSemaphore.acquire();
    }

    protected void lockGranted() {
        System.out.println("Node " + nodeId + " has the lock!");
        System.out.println("Releasing lock semaphore for ID " + this.nodeId);
        this.acquireLockSemaphore.release();
    }

    public void initiateUnlock() throws IOException { // rule 6
        System.out.println("Initiating UNLOCK for ID " + this.nodeId);
        for (Integer IDs : this.delayed.keySet()) {
            if (this.delayed.get(IDs)) {
                Packet ongoing = new Packet(PacketType.ACK_LOCK);
                ongoing.setData(this.nodeId, this.LC);
                this.channel.basicPublish("", String.valueOf(IDs), null, ongoing.toByteArray());
                this.delayed.replace(IDs, false);
            }
        }
        this.asking = false;
        this.nb_perm = 0;
        this.concurrentOperationSemaphore.release();
    }
}
