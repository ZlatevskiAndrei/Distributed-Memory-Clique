package ids.alet_zlatevski.controller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MutualExclusionHandler {

    protected final String brokerHostname;

    public MutualExclusionHandler(final String brokerHostname) {
        this.brokerHostname = brokerHostname;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerHostname);
        Channel channel;
        Connection connection;
        long pid = ProcessHandle.current().pid();
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare("BROADCAST", false, true, true, null);
            channel.queueDeclare(String.valueOf(pid), false, true, true, null);

        } catch (IOException e) {
            System.out.println("IO Exception at " + this.brokerHostname);
            System.out.println("Exiting!");
            System.exit(130);
        } catch (TimeoutException t) {
            t.printStackTrace();
        }
    }
}
