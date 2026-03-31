package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.util.Objects;

import static ids.alet_zlatevski.env.Constants.BROADCAST_EXCHANGE;

public class BroadcastDispatcher implements Consumer {


    protected MutualExclusionHandler owner;

    public BroadcastDispatcher(MutualExclusionHandler owner) {
        this.owner = owner;
    }

    @Override
    public void handleConsumeOk(String consumerTag) {

    }

    @Override
    public void handleCancelOk(String consumerTag) {

    }

    @Override
    public void handleCancel(String consumerTag) throws IOException {

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
            case HANDSHAKE:
                String newNodeId = packet.getString();
                if (Objects.equals(this.owner.getNodeId(), newNodeId)) { // we don't send ack to ourself
                    return;
                }

                Packet ownIdPacket = new Packet(PacketType.HANDSHAKE);
                ownIdPacket.setData(this.owner.getNodeId());
                // we send on the other broadcast channel our own id
                this.owner.channel.basicPublish("BROADCAST", newNodeId, null, ownIdPacket.toByteArray());

                assert (!this.owner.nodeIds.contains(newNodeId));
                this.owner.nodeIds.add(newNodeId);
                assert (this.owner.nodeIds.size() <= this.owner.nodeNumber);
                System.out.println("Current value : " + this.owner.nodeIds.toString());
                if (this.owner.nodeIds.size() == this.owner.nodeNumber) {
                    this.owner.handShakeCallback();
                }
                break;
            default:
                break;
        }
    }
}
