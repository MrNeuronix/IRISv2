package fi.ceci.client;

import org.apache.log4j.BasicConfigurator;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.AMQConnection;
import org.junit.Test;

import javax.jms.*;

/**
 * Test class for testing qpid client.
 */
public class QpidClientTest {

    static {
        BasicConfigurator.configure();
    }

    @Test
    public void statusQueryTest() throws Exception, Throwable {
        final Connection connection = new AMQConnection(
                "amqp://admin:admin@localhost/?brokerlist='tcp://localhost:5672'");
        connection.start ();

        final Session session = connection.createSession (false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = new AMQAnyDestination ("ADDR:iris; {create: always, node: {type: topic}}");
        final MessageConsumer messageConsumer = session.createConsumer (destination);
        final MessageProducer messageProducer = session.createProducer (destination);

        MapMessage message = session.createMapMessage();
        message.setStringProperty ("qpid.subject", "status.all");
        messageProducer.send (message);

        for (int i = 0; i<100 ;i++) {
            final Message receivedMessage = (Message) messageConsumer.receive();
            System.out.println(receivedMessage);
            Thread.sleep(100);
        }
    }

}
