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
                if (!owner.asking) { // rule 2

                    owner.LC = Math.max(owner.LC, distant.LD()) + 1;

                    Packet ongoing = new Packet(PacketType.ACK_LOCK)
                            .setData(owner.nodeId, owner.LC);

                    this.owner.channel.basicPublish("", distant.ID(), null, ongoing.toByteArray());
                } else if (owner.LD > distant.LD() || owner.LD == distant.LD()
                        && Integer.parseInt(owner.nodeId) > Integer.parseInt(distant.ID())) { // rule 3

                    owner.LC = Math.max(owner.LC, distant.LD()) + 1;

                    owner.delayed.put(Integer.parseInt(distant.ID()), true);
                } else { // rule 4
                    owner.LC = Math.max(owner.LC, distant.LD()) + 1;

                    Packet ongoing = new Packet(PacketType.ACK_LOCK)
                            .setData(owner.nodeId, owner.LC);

                    this.owner.channel.basicPublish("", distant.ID(), null, ongoing.toByteArray());
                }
            }

            case ACK_LOCK -> { // rule 5
                Packet.IDAndLC distant = packet.getNodeIdAndLD();

                owner.LC = Math.max(owner.LC, distant.LD()) + 1;
                owner.nb_perm += 1;

                if (owner.nb_perm == NODE_NUMBER - 1) {
                    owner.lockGranted(); // we expect perms from everyone except us
                }
            }

            case GLOBAL_UPDATE -> {
                Packet.ID_ADDR_VAL distant = packet.getIdAddrVal();
                try {
                    owner.distantMemoryUpdate(distant.addr(), distant.val());
                    Packet newPacket = new Packet(PacketType.ACK_GLOBAL_UPDATE);
                    this.owner.channel.basicPublish("", distant.ID(), null, newPacket.toByteArray());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            case ACK_GLOBAL_UPDATE -> {
                owner.global_update_acks += 1;
                if(owner.global_update_acks == NODE_NUMBER - 1) {
                    owner.globalUpdateFinished();
                }
            }

            default -> System.out.println("ayy");
        }
    }
}
