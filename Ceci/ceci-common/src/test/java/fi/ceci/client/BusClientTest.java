package fi.ceci.client;

import fi.ceci.model.Bus;
import org.apache.log4j.BasicConfigurator;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.messaging.Address;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.PostalAddress;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Test class for resting ago control client.
 */
public class BusClientTest {
    /** The properties category used in instantiating default services. */
    private static final String PROPERTIES_CATEGORY = "test";
    /** The persistence unit to be used. */
    public static final String PERSISTENCE_UNIT = "ago-control-vaadin-site";
    /** The entity manager factory for test. */
    private static EntityManagerFactory entityManagerFactory;

    static {
        BasicConfigurator.configure();
        @SuppressWarnings("rawtypes")
        final Map properties = new HashMap();
        properties.put(PersistenceUnitProperties.JDBC_DRIVER,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_DRIVER));
        properties.put(PersistenceUnitProperties.JDBC_URL,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_URL));
        properties.put(PersistenceUnitProperties.JDBC_USER,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_USER));
        properties.put(PersistenceUnitProperties.JDBC_PASSWORD,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_PASSWORD));
        properties.put(PersistenceUnitProperties.DDL_GENERATION,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.DDL_GENERATION));
        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }

    /** The entity manager for test. */
    private EntityManager entityManager;
    private Company owner;

    /**
     * @throws Exception if exception occurs in setup.
     */
    @Before
    public void setUp() throws Exception {

        entityManager = entityManagerFactory.createEntityManager();

        entityManager.getTransaction().begin();
        final PostalAddress invoicingAddress = new PostalAddress("", "", "", "", "", "");
        final PostalAddress deliveryAddress = new PostalAddress("", "", "", "", "", "");
        entityManager.persist(invoicingAddress);
        entityManager.persist(deliveryAddress);
        owner = new Company("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", invoicingAddress, deliveryAddress);
        entityManager.persist(owner);
        entityManager.getTransaction().commit();
    }

    @Test
    @Ignore
    public void testQpidClient() throws Exception {
        final Bus bus = new Bus();
        bus.setOwner(owner);
        final BusClient client = new BusClient(entityManagerFactory, bus);

        while (true) {
            Thread.sleep(100);
        }
    }

    @Test
    @Ignore
    public void testInventorySynchronization() throws Exception, Throwable {
        final String userName = "agocontrol";
        final String password = "letmein";

        final Properties properties = new Properties();
        properties.put("java.naming.factory.initial",
                "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        properties.put("connectionfactory.qpidConnectionfactory",
                "amqp://" + userName + ":" + password + "@clientid2/test2?brokerlist='tcp://localhost:5672'");
        //properties.put("destination.topicExchange","agocontrol/#");

        final Context context = new InitialContext(properties);
        final ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
        final Connection connection = connectionFactory.createConnection();

        connection.start();

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Queue replyQueue = session.createTemporaryQueue();
        final Destination sendDestination = new AMQAnyDestination(new Address("agocontrol", "", null));
        final Destination receiveDestination = new AMQAnyDestination(new Address("agocontrol", "#", null));

        final MessageProducer messageProducer = session.createProducer(sendDestination);
        final MessageConsumer messageConsumer = session.createConsumer(replyQueue);

        final MapMessage message = session.createMapMessage();
        message.setJMSMessageID("ID:" + UUID.randomUUID().toString());
        message.setJMSCorrelationID("ID:" + UUID.randomUUID().toString());
        message.setString("uuid", "9d1abba9-cd75-4a12-929c-ef28e1965fc4");
        message.setString("id", "1");
        message.setString("command", "on");
        message.setJMSReplyTo(replyQueue);
        messageProducer.send(message);

        for (int i = 0; i<100 ;i++) {
            final Message receivedMessage = (Message) messageConsumer.receive();
            System.out.println(receivedMessage);
            Thread.sleep(100);
        }

        session.close();
        connection.close();
        context.close();
    }

}
