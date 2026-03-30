package ids.alet_zlatevski.controller.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MutualExclusionHandler {

    protected final int nodeNumber;

    protected final String brokerHostname;
    protected final Connection connection;
    protected final Channel channel;

    protected final String[] nodeIds;

    public MutualExclusionHandler(final String brokerHostname, int nodeNumber) throws IOException, TimeoutException {
        this.brokerHostname = brokerHostname;
        this.nodeNumber = nodeNumber;
        this.nodeIds = new String[nodeNumber - 1];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerHostname);
        long pid = ProcessHandle.current().pid();
        this.connection = factory.newConnection();

        this.channel = connection.createChannel();

        this.channel.queueDeclare("BROADCAST", false, true, true, null);
        this.channel.queueDeclare(String.valueOf(pid), false, true, true, null);

        this.setupCallbacks();
    }


    protected void setupCallbacks() {

    }

    protected void handShakeCallback() {
        // TODO here ALL NODE ARE CONNECTED !
    }


}
