package ru.iris.events;

import com.avaje.ebean.Ebean;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import ru.iris.common.database.model.Event;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

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

			// load events from db
			List<Event> events = Ebean.find(Event.class).findList();

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

                        for (Event event : events) {

							String[] subjects = event.getSubject().split(",");
							Set<String> set = new HashSet<String>(Arrays.asList(subjects));

							if (wildCardMatch(set, envelope.getSubject()))
							{
								File jsFile = new File("./scripts/" + event.getScript());

								log.debug("Launch script: " + event.getScript());

								try
								{
									ScriptableObject.putProperty(scope, "advertisement", Context.javaToJS(envelope.getObject(), scope));
									cx.evaluateString(scope, FileUtils.readFileToString(jsFile), jsFile.toString(), 1, null);
								}
								catch (FileNotFoundException e)
								{
									log.error("Script file " + jsFile + " not found!");
								}
								catch (Exception e)
								{
									log.error("Error in script " + jsFile + ": " + e.toString());
									e.printStackTrace();
								}
							}
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

	private boolean wildCardMatch(Set<String> patterns, String text)
	{

		// add sentinel so don't need to worry about *'s at end of pattern
		for (String pattern : patterns)
		{
			text += '\0';
			pattern += '\0';

			int N = pattern.length();

			boolean[] states = new boolean[N + 1];
			boolean[] old = new boolean[N + 1];
			old[0] = true;

			for (int i = 0; i < text.length(); i++)
			{
				char c = text.charAt(i);
				states = new boolean[N + 1]; // initialized to false
				for (int j = 0; j < N; j++)
				{
					char p = pattern.charAt(j);

					// hack to handle *'s that match 0 characters
					if (old[j] && (p == '*'))
						old[j + 1] = true;

					if (old[j] && (p == c))
						states[j + 1] = true;
					if (old[j] && (p == '*'))
						states[j] = true;
					if (old[j] && (p == '*'))
						states[j + 1] = true;
				}
				old = states;
			}
			if (states[N])
				return true;
		}
		return false;
	}
}
