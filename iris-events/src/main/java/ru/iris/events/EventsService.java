package ru.iris.events;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import ru.iris.common.SQL;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 19.11.13
 * Time: 11:25
 * License: GPL v3
 */

public class EventsService implements Runnable {

    private Logger log = LogManager.getLogger(EventsService.class.getName());
    private boolean shutdown = false;
    private SQL sql = Service.getSQL();

    public EventsService() {
        Thread t = new Thread(this);
        t.setName("Event Service");
        t.start();
    }

    @Override
    public synchronized void run() {

        try {

            ServiceCheckEmitter serviceCheckEmitter = new ServiceCheckEmitter("Events");
            serviceCheckEmitter.setState(ServiceStatus.STARTUP);

            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

            // Initialize rhino engine
            Global global = new Global();
            Context cx = ContextFactory.getGlobal().enterContext();
            global.init(cx);
            Scriptable scope = cx.initStandardObjects(global);

            // Pass jsonmessaging instance to js engine
            ScriptableObject.putProperty(scope, "jsonMessaging", Context.javaToJS(jsonMessaging, scope));
            ScriptableObject.putProperty(scope, "out", Context.javaToJS(System.out, scope));

            // filter js files
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".js");
                }
            };

            // subscribe to anything
            jsonMessaging.subscribe("*");
            jsonMessaging.start();

            serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

            while (!shutdown) {

                JsonEnvelope envelope = jsonMessaging.receive(100);

                if (envelope != null) {

                    // Check command and launch script
                    if (envelope.getObject() instanceof CommandAdvertisement) {
                        CommandAdvertisement advertisement = envelope.getObject();
                        log.info("Launch command script: " + advertisement.getScript());

                        File jsFile = new File("./scripts/command/" + advertisement.getScript() + ".js");

                        try {
                            ScriptableObject.putProperty(scope, "commandParams", Context.javaToJS(advertisement.getData(), scope));
                            cx.evaluateString(scope, FileUtils.readFileToString(jsFile), jsFile.toString(), 1, null);
                        } catch (FileNotFoundException e) {
                            log.error("Script file scripts/command/" + advertisement.getScript() + ".js not found!");
                        } catch (Exception e) {
                            log.error("Error in script scripts/command/" + advertisement.getScript() + ".js: " + e.toString());
                            e.printStackTrace();
                        }
                    } else {

                        // seek event in db
                        ResultSet rs = sql.select("SELECT * FROM events WHERE subject='" + envelope.getSubject() + "'");

                        try {
                            while (rs.next()) {

                                File jsFile = new File("./scripts/" + rs.getString("script"));

                                log.debug("Launch script: " + rs.getString("script"));

                                try {
                                    ScriptableObject.putProperty(scope, "advertisement", Context.javaToJS(envelope.getObject(), scope));
                                    cx.evaluateString(scope, FileUtils.readFileToString(jsFile), jsFile.toString(), 1, null);
                                } catch (FileNotFoundException e) {
                                    log.error("Script file " + jsFile + " not found!");
                                } catch (Exception e) {
                                    log.error("Error in script " + jsFile + ": " + e.toString());
                                    e.printStackTrace();
                                }
                            }

                            rs.close();

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // Broadcast that this service is shutdown.
            serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Events", t);
        }

    }
}
