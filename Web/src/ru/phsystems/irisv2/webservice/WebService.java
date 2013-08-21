package ru.phsystems.irisv2.webservice;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 * License: GPL v3
 */

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class WebService implements Runnable {

    Thread t = null;
    private static Logger log = LoggerFactory.getLogger(WebService.class.getName());

    public WebService() {
        t = new Thread(this);
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    @Override
    public synchronized void run() {

        log.info("[web] Service started");

        Properties prop = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream("./conf/main.property");
            prop.load(is);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Jetty

        try {

            log.info("[web] Configured to run on port " + prop.getProperty("httpPort"));

            // System.setProperty("org.eclipse.jetty.http.LEVEL", "WARN");
            Server server = new Server(Integer.valueOf(prop.getProperty("httpPort")));

            // Тут определяется контекст для контроллера
            ServletContextHandler context0 = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context0.setSecurityHandler(basicAuth(prop.getProperty("httpUser"), prop.getProperty("httpPassword"), "IRIS-X request authorization"));
            context0.setContextPath("/control");
            context0.addServlet(new ServletHolder(new ControlHandler()), "/zwave/*");
            context0.addServlet(new ServletHolder(new VideoHandler()), "/video/*");
            context0.addServlet(new ServletHolder(new AudioHandler()), "/audio/*");
            //context0.addServlet(new ServletHolder(new SpeakHandler()), "/speak/*");
            //context0.addServlet(new ServletHolder(new DeviceValuesHandler()), "/device/values/*");
            //context0.addServlet(new ServletHolder(new DeviceHandler()), "/device/*");
            //context0.addServlet(new ServletHolder(new ScheduleHandler()), "/scheduler/*");

            ServletContextHandler context1 = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context1.setSecurityHandler(basicAuth(prop.getProperty("httpUser"), prop.getProperty("httpPassword"), "IRIS-X request authorization"));
            context1.setContextPath("/");
            context1.addServlet(new ServletHolder(new HTMLHandler()), "/*");

            // Тут определяется контекст для статики (html, cs, картинки и т.д.)
            ResourceHandler resource_handler = new ResourceHandler();
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setSecurityHandler(basicAuth(prop.getProperty("httpUser"), prop.getProperty("httpPassword"), "IRIS-X request authorization"));
            context.setContextPath("/static");
            context.setResourceBase("./www/");
            context.setClassLoader(Thread.currentThread().getContextClassLoader());
            context.setHandler(resource_handler);

            // Выставляем контексты в коллекцию
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.setHandlers(new Handler[]{context0, context1, context});

            // Назначаем коллекцию контекстов серверу и запускаем его
            server.setHandler(contexts);
            server.start();
            server.join();

        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    // Авторизация
    private static final SecurityHandler basicAuth(String username, String password, String realm) {

        HashLoginService l = new HashLoginService();
        l.putUser(username, Credential.getCredential(password), new String[]{"user"});
        l.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("myrealm");
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;
    }
}
