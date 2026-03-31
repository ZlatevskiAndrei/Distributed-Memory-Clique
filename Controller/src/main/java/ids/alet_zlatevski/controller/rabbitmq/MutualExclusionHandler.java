package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.BROADCAST_EXCHANGE;

public class MutualExclusionHandler {

    protected final int nodeNumber;

    protected final String brokerHostname;
    protected final Connection connection;
    protected final Channel channel;

    protected final ArrayList<String> nodeIds;
    protected final String nodeId;

    public MutualExclusionHandler(final String brokerHostname, int nodeNumber, String nodeId) throws IOException, TimeoutException {
        this.brokerHostname = brokerHostname;
        this.nodeNumber = nodeNumber;
        this.nodeIds = new ArrayList<>();
        this.nodeId = nodeId;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerHostname);
        this.connection = factory.newConnection();

        this.channel = connection.createChannel();
        this.channel.exchangeDeclare(BROADCAST_EXCHANGE, "fanout");


        this.channel.queueDeclare("BROADCAST", false, true, true, null);
        this.channel.queueBind("BROADCAST", BROADCAST_EXCHANGE, "");

        this.channel.queueDeclare(nodeId, false, true, true, null);

        this.setupCallbacks();
    }


    protected void setupCallbacks() {

    }

    protected void initiateMutualExlusion() throws IOException {
        Packet packet = new Packet(PacketType.HANDSHAKE);
        packet.setData(nodeId);

        this.channel.basicPublish(BROADCAST_EXCHANGE, "", null, packet.toByteArray());
        this.channel.basicConsume("BROADCAST", new BroadcastDispatcher(this));
        //here we call a basic consume to the receiving queue.
    }

    protected void handShakeCallback() { //should put this method maybe in Controller, makes more sense
        // TODO here ALL NODE ARE CONNECTED !
    }

    public String getNodeId() {
        return nodeId;
    }
}
