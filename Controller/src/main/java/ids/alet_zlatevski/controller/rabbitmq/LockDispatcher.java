package ids.alet_zlatevski.controller.rabbitmq;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;



public class LockDispatcher implements Consumer{
    protected MutualExclusionHandler owner;

    public LockDispatcher(MutualExclusionHandler owner) {
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
        String[] data = packet.parseTwoStringData(packet.getString());
        String distantNodeId = data[0];
        int distantLD;

        switch (packet.getType()) {
            case LOCK:
                distantLD = Integer.parseInt(data[1]);
                if(!owner.asking) { //rule 2
                    owner.LC = Math.max(owner.LC, distantLD) + 1;
                    Packet ongoing = new Packet(PacketType.ACK);
                    ongoing.setData(owner.nodeId + "," + owner.LC);
                    this.owner.channel.basicPublish("", distantNodeId, null, ongoing.toByteArray());
                }
                else if(owner.LD > distantLD || owner.LD == distantLD && Integer.parseInt(owner.nodeId) > Integer.parseInt(distantNodeId)) { //rule 3
                    owner.LC = Math.max(owner.LC, distantLD) + 1;
                    owner.delayed.put(Integer.parseInt(distantNodeId), true);
                }
                else { //rule 4
                    owner.LC = Math.max(owner.LC, distantLD) + 1;
                    Packet ongoing = new Packet(PacketType.ACK);
                    ongoing.setData(owner.nodeId + "," + owner.LC);
                    this.owner.channel.basicPublish("", distantNodeId, null, ongoing.toByteArray());
                }
                break;

            case ACK: // rule 5
                distantLD = Integer.parseInt(data[1]);
                owner.LC = Math.max(owner.LC, distantLD) + 1;
                owner.nb_perm +=1;
                if (owner.nb_perm == owner.nodeNumber - 1) owner.lockGranted(); //we expect perms from everyone except us
                break;

            default:
                System.out.println("ayy");
                break;
        }
    }
}

