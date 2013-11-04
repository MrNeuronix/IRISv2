/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.ceci.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import fi.ceci.client.BusClient;
import fi.ceci.client.BusClientManager;
import fi.ceci.client.EventProcessor;
import fi.ceci.model.Bus;
import fi.ceci.ui.viewlet.EventViewlet;
import fi.ceci.ui.viewlet.LargeImageViewlet;
import fi.ceci.ui.viewlet.StatusViewlet;
import fi.ceci.ui.viewlet.bus.BusFlowViewlet;
import fi.ceci.ui.viewlet.element.ElementFlowViewlet;
import fi.ceci.ui.viewlet.record.RecordFlowViewlet;
import fi.ceci.ui.viewlet.recordset.RecordSetFlowViewlet;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.*;
import org.vaadin.addons.sitekit.util.PersistenceUtil;
import org.vaadin.addons.sitekit.util.PropertiesUtil;
import org.vaadin.addons.sitekit.viewlet.administrator.company.CompanyFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.customer.CustomerFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.group.GroupFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.user.UserFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.*;
import org.vaadin.addons.sitekit.viewlet.anonymous.login.LoginFlowViewlet;
import org.vaadin.addons.sitekit.web.BareSiteFields;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * BareSite UI.
 *
 * @author Tommi S.E. Laukkanen
 */
@SuppressWarnings({ "serial", "unchecked" })
@Theme("ceci")
public final class CeciUI extends AbstractSiteUI implements ContentProvider {

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(CeciUI.class);
    /** The properties category used in instantiating default services. */
    private static final String PROPERTIES_CATEGORY = "conf/ceci";
    /** The persistence unit to be used. */
    public static final String PERSISTENCE_UNIT = "ceci";
    /** The localization bundle to be used. */
    public static final String LOCALIZATION_BUNDLE = "ceci-localization";
    /** The bus client. */
    private static BusClientManager busClientManager;

    /**
     * Main method for running BareSiteUI.
     * @param args the commandline arguments
     * @throws Exception if exception occurs in jetty startup.
     */
    public static void main(final String[] args) throws Exception {
        PropertiesUtil.setCategoryRedirection("bare-site", "ceci");
        final Thread mainThread = Thread.currentThread();
        DOMConfigurator.configure("./conf/etc/log4j.xml");

        entityManagerFactory = PersistenceUtil.getEntityManagerFactory(PERSISTENCE_UNIT, PROPERTIES_CATEGORY);

        final String webappUrl = CeciUI.class.getClassLoader().getResource("webapp/").toExternalForm();
        final int port = Integer.parseInt(PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "port"));
        final Server server = new Server(port);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setDescriptor(webappUrl + "/WEB-INF/web.xml");
        context.setResourceBase(webappUrl);
        context.setParentLoaderPriority(true);
        context.setWelcomeFiles(new String[] {"/ceci"});
        context.setInitParameter("org.eclipse.jetty.servlet.Default.redirectWelcome", "true");

        server.setHandler(context);
        server.start();

        busClientManager = new BusClientManager(entityManagerFactory);

        final EventProcessor eventProcessor = new EventProcessor(entityManagerFactory);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    eventProcessor.close();
                } catch (final Throwable t) {
                    LOGGER.error("Error in event processor stop.", t);
                }
                try {
                    busClientManager.close();
                } catch (final Throwable t) {
                    LOGGER.error("Error in bus client manager stop.", t);
                }
                    try {
                    server.stop();
                } catch (final Throwable t) {
                    LOGGER.error("Error in jetty server stop.", t);
                }
            }
        });

        server.join();
    }

    /**
     * Get bus client.
     * @param bus the bus
     * @return the bus client
     */
    public BusClient getBusClient(final Bus bus) {
        return busClientManager.getBusClient(bus);
    }

    @Override
    protected Site constructSite(final VaadinRequest request) {
        setPollInterval(5000);
        final ContentProvider contentProvider = this;

        final LocalizationProvider localizationProvider =
                new LocalizationProviderBundleImpl(new String[] {"bare-site-localization",
                        LOCALIZATION_BUNDLE});

        BareSiteFields.initialize(localizationProvider, getLocale());
        CeciFields.initialize(localizationProvider, getLocale());

        final SiteContext siteContext = new SiteContext();
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        siteContext.putObject(EntityManagerFactory.class, entityManagerFactory);
        siteContext.putObject(EntityManager.class, entityManager);

        Company company = CompanyDao.getCompany(entityManager,
                ((VaadinServletRequest) VaadinService.getCurrentRequest()).getHttpServletRequest().getServerName());
        if (company == null) {
            // If no exact host match exists then try to find global company marked with *.
            company = CompanyDao.getCompany(entityManager, "*");
        }
        siteContext.putObject(Company.class, company);

        final SecurityProviderSessionImpl securityProvider = new SecurityProviderSessionImpl(
                Arrays.asList("administrator", "user"));

        final Map<String, Boolean> statuses = new LinkedHashMap<String, Boolean>();
        statuses.put("database", true);
        statuses.put("connectivity", busClientManager.isConnected());
        statuses.put("inventory", false);
        siteContext.putObject("statuses", statuses);

        return new Site(SiteMode.PRODUCTION, contentProvider, localizationProvider, securityProvider, siteContext);
    }

    @Override
    public SiteDescriptor getSiteDescriptor() {
        final List<ViewDescriptor> viewDescriptors = new ArrayList<ViewDescriptor>();

        viewDescriptors.add(new ViewDescriptor("master", null, null, new ViewVersion(0, null, "Master", "",
                "This is a master view.", WideView.class.getCanonicalName(), new String[]{"admin"},
                Arrays.asList(
                        new ViewletDescriptor("logo", "Logo", "This is logo.", "ceci.png",
                                ImageViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("header", "Header", "This is header.", null,
                                CompanyHeaderViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("navigation", "NavigationDescriptor", "This is navigation.", null,
                                NavigationViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("footer", "Footer", "This is footer.", null,
                                CompanyFooterViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("default", null, null, new ViewVersion(0, "master", "Default", "",
                "This is default view.", NarrowView.class.getCanonicalName(), new String[]{},
                Arrays.asList(
                        new ViewletDescriptor("logo", "Logo", "This is logo.", "ceci.png",
                                LargeImageViewlet.class.getCanonicalName()),
                        new ViewletDescriptor(
                                "status", "Status", "Providers status ui.", null,
                                StatusViewlet.class.getCanonicalName()),
                        new ViewletDescriptor(
                                "left", "Events", "Events.", null,
                                EventViewlet.class.getCanonicalName()),
                        new ViewletDescriptor(
                                "right", "Events", "Events.", null,
                                EventViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("navigation", "NavigationDescriptor", "This is navigation.", null,
                                fi.ceci.ui.viewlet.NavigationViewlet.class.getCanonicalName())
                )
        )));

        viewDescriptors.add(new ViewDescriptor("buses", null, null, new ViewVersion(
                0, "master", "Buses", "", "This is buses page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Buses Viewlet", "This is Buses viewlet.", null,
                        BusFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("elements", null, null, new ViewVersion(
                0, "master", "Elements", "", "This is elements page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Elements Viewlet", "This is Elements viewlet.", null,
                         ElementFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("records", null, null, new ViewVersion(
                0, "master", "Records", "", "This is records page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Records Viewlet", "This is Records viewlet.", null,
                        RecordFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("record-sets", null, null, new ViewVersion(
                0, "master", "Record Sets", "", "This is record sets page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Record Sets Viewlet", "This is Record Sets viewlet.", null,
                        RecordSetFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("users", null, null, new ViewVersion(
                0, "master", "Users", "", "This is users page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        UserFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("groups", null, null, new ViewVersion(
                0, "master", "Groups", "", "This is groups page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        GroupFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("customers", null, null, new ViewVersion(
                0, "master", "Customers", "customers", "This is customers page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        CustomerFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("companies", null, null, new ViewVersion(
                0, "master", "Companies", "companies", "This is companies page.",
                WideView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        CompanyFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("login", null, null, new ViewVersion(
                0, "master", "Login SiteView", "login page", "This is login page.",
                WideView.class.getCanonicalName(), new String[]{"anonymous"},
                Arrays.asList(
                        new ViewletDescriptor(
                                "content", "Flowlet Sheet", "This is flow sheet.", null,
                                LoginFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("validate", null, null, new ViewVersion(
                0, "master", "Email Validation", "email validation page", "This is email validation page.",
                WideView.class.getCanonicalName(), new String[]{"anonymous"},
                Arrays.asList(
                        new ViewletDescriptor(
                                "content", "Email Validation", "This is email validation flowlet.", null,
                                EmailValidationViewlet.class.getCanonicalName())
                ))));

        final NavigationDescriptor navigationDescriptor = new NavigationDescriptor("navigation", null, null,
                new NavigationVersion(0, "default",
                        "default;buses;elements;records;record-sets;customers;users;groups;companies;login", true));

        return new SiteDescriptor("Test site.", "test site", "This is a test site.",
                navigationDescriptor, viewDescriptors);

    }

    /** The entity manager factory for test. */
    private static EntityManagerFactory entityManagerFactory;

}
