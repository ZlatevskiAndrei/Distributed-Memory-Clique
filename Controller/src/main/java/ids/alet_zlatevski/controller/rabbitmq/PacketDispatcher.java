package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import ids.alet_zlatevski.IO;


import java.io.IOException;
import java.util.Objects;

import static ids.alet_zlatevski.env.Constants.NODE_NUMBER;

public class PacketDispatcher implements Consumer {


    protected MutualExclusionHandler owner;

    public PacketDispatcher(MutualExclusionHandler owner) {
        this.owner = owner;
    }

    @Override
    public void handleConsumeOk(String consumerTag) {

    }

    @Override
    public void handleCancelOk(String consumerTag) {

    }

    @Override
    public void handleCancel(String consumerTag) {

    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {

    }

    @Override
    public void handleRecoverOk(String consumerTag) {

    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        Packet packet = new Packet(body);

        switch (packet.getType()) {

            case HANDSHAKE -> {
                String newNodeId = packet.getString();

                // we don't send ack to ourselves or if the node was already added.
                if (Objects.equals(this.owner.getNodeId(), newNodeId) || this.owner.nodeIds.contains(newNodeId)) {
                    return;
                }

                // we send on the other broadcast channel our own id
                Packet ownIdPacket = new Packet(PacketType.HANDSHAKE);
                ownIdPacket.setData(this.owner.getNodeId());
                this.owner.channel.basicPublish("", newNodeId, null, ownIdPacket.toByteArray());

                this.owner.nodeIds.add(newNodeId);
                IO.println("Current saved ids : " + this.owner.nodeIds);

                // execute the callback if needed.
                if (this.owner.nodeIds.size() == NODE_NUMBER - 1) {
                    this.owner.handshakeFinished();
                }
            }

            case LOCK -> {
                Packet.IDAndLC distant = packet.getNodeIdAndLD();
                if (Objects.equals(distant.ID(), owner.nodeId)) {
                    return; // Ignore our own request
                }
                
                owner.LC = Math.max(owner.LC, distant.LD()) + 1; // Update logical clock
                
                if (!owner.asking) { // Rule 2
                    Packet ongoing = new Packet(PacketType.ACK_LOCK)
                            .setData(owner.nodeId, owner.LC);
                    this.owner.channel.basicPublish("", distant.ID(), null, ongoing.toByteArray());
                } else if (owner.LD < distant.LD() || (owner.LD == distant.LD() && Integer.parseInt(owner.nodeId) < Integer.parseInt(distant.ID()))) {
                    // Rule 3: Current node has priority (smaller timestamp or smaller ID)
                    // Delay the reply
                    owner.delayed.put(Integer.parseInt(distant.ID()), true);
                } else {
                    // Rule 4: Incoming node has priority
                    Packet ongoing = new Packet(PacketType.ACK_LOCK)
                            .setData(owner.nodeId, owner.LC);
                    this.owner.channel.basicPublish("", distant.ID(), null, ongoing.toByteArray());
                }
            }

            case ACK_LOCK -> { // Rule 5
                Packet.IDAndLC distant = packet.getNodeIdAndLD();
                if (Objects.equals(distant.ID(), owner.nodeId)) {
                    return; // Ignore our own ACK
                }
                
                owner.LC = Math.max(owner.LC, distant.LD()) + 1;
                owner.nb_perm += 1;

                if (owner.nb_perm >= owner.nodeIds.size()) {
                    owner.lockGranted();
                }
            }

            case GLOBAL_UPDATE -> {
                Packet.ID_ADDR_VAL distant = packet.getIdAddrVal();
                // We should only process updates from other nodes
                if (Objects.equals(distant.ID(), owner.nodeId)) {
                    return;
                }
                
                try {
                    owner.distantMemoryUpdate(distant.addr(), distant.val());
                    Packet ackPacket = new Packet(PacketType.ACK_GLOBAL_UPDATE);
                    ackPacket.setData(this.owner.getNodeId()); // Add our ID to the ACK
                    this.owner.channel.basicPublish("", distant.ID(), null, ackPacket.toByteArray());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            case ACK_GLOBAL_UPDATE -> {
                String senderId = packet.getString();
                // Only count ACKs from other nodes
                if (!Objects.equals(senderId, owner.nodeId)) {
                    owner.global_update_acks += 1;
                    if (owner.global_update_acks >= owner.nodeIds.size()) {
                        owner.globalUpdateFinished();
                    }
                }
            }

            default -> System.out.println("ayy");
        }
    }
}
