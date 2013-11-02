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
package fi.ceci.ui.viewlet.dashboard;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import fi.ceci.dao.ElementDao;
import fi.ceci.dao.RecordDao;
import fi.ceci.dao.RecordSetDao;
import fi.ceci.model.Element;
import fi.ceci.model.Record;
import fi.ceci.model.RecordSet;
import fi.ceci.ui.CeciUI;
import fi.ceci.util.DisplayValueConversionUtil;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The building control panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BuildingControlPanel extends AbstractViewlet {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BuildingControlPanel.class);

    /**
     * The layout.
     */
    private final VerticalLayout layout;
    /**
     * The elementLayout.
     */
    private final VerticalLayout elementLayout;
    /**
     * The site.
     */
    private final Site site;
    /**
     * The site context.
     */
    private final SiteContext siteContext;
    /**
     * The entity manager.
     */
    private final EntityManager entityManager;
    /**
     * The room icon.
     */
    private final Resource roomIcon;
    /**
     * The device icon.
     */
    private final Resource deviceIcon;
    /**
     * The temperature icon.
     */
    private final Resource temperatureIcon;
    /**
     * The brightness icon.
     */
    private final Resource brightnessIcon;
    /**
     * The humidity icon.
     */
    private final Resource humidityIcon;
    /**
     * The event icon.
     */
    private final Resource eventIcon;
    /**
     * The record layouts.
     */
    private final Map<String, GridLayout> recordsLayouts = new HashMap<String, GridLayout>();
    /**
     * The record layouts.
     */
    private final BlockingQueue<List<Record>> recordsQueue = new LinkedBlockingQueue<List<Record>>();
    /**
     * True if record reader should exit.
     */
    private boolean recordReaderExitRequested = false;
    /**
     * The record thread.
     */
    private Thread recordReaderThread = null;


    /**
     * Default constructor.
     */
    public BuildingControlPanel() {
        site = ((CeciUI) UI.getCurrent()).getSite();
        siteContext = site.getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.setStyleName(Reindeer.LAYOUT_WHITE);

        final Label title = new Label("Control Panel");
        title.setIcon(getSite().getIcon("inventory"));
        title.setStyleName(Reindeer.LABEL_H2);
        layout.addComponent(title);
        layout.setExpandRatio(title, 0);

        elementLayout = new VerticalLayout();
        elementLayout.setSpacing(true);
        elementLayout.setMargin(false);
        layout.addComponent(elementLayout);
        layout.setExpandRatio(elementLayout, 1);

        roomIcon = site.getIcon("room");
        deviceIcon = site.getIcon("device");
        temperatureIcon = site.getIcon("temperature");
        brightnessIcon = site.getIcon("brightness");
        humidityIcon = site.getIcon("humidity");
        eventIcon = site.getIcon("event");

        setCompositionRoot(layout);

        // the Refresher polls automatically
        final Refresher refresher = new Refresher();
        refresher.setRefreshInterval(200);
        refresher.addListener(new Refresher.RefreshListener() {
            @Override
            public void refresh(final Refresher refresher) {
                while (!recordsQueue.isEmpty()) {
                    final List<Record> records = recordsQueue.remove();
                    if (records.size() > 0) {
                        final Record record = records.get(0);
                        final RecordSet recordSet = record.getRecordSet();
                        final Element element = recordSet.getElement();

                        final GridLayout recordsLayout = recordsLayouts.get(element.getElementId());
                        if (recordsLayout == null) {
                            continue;
                        }

                        final int columnIndex = recordSet.getType().ordinal();
                        final int rowIndex = 0;
                        if (recordsLayout.getComponent(columnIndex, rowIndex) != null) {
                            continue;
                        }

                        final VerticalLayout recordLayout = new VerticalLayout();
                        recordLayout.setSpacing(true);
                        final Resource recordIcon;
                        switch (recordSet.getType()) {
                            case TEMPERATURE:
                                recordIcon = temperatureIcon;
                                break;
                            case BRIGHTNESS:
                                recordIcon = brightnessIcon;
                                break;
                            case HUMIDITY:
                                recordIcon = humidityIcon;
                                break;
                            default:
                                recordIcon = eventIcon;
                                break;
                        }

                        final Embedded embedded = new Embedded(null, recordIcon);
                        recordLayout.addComponent(embedded);
                        recordLayout.setExpandRatio(embedded, 0.1f);
                        embedded.setWidth(32, Unit.PIXELS);
                        embedded.setHeight(32, Unit.PIXELS);


                        final Label label = new Label();
                        recordLayout.addComponent(label);
                        recordLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);

                        final String recordUnit = recordSet.getUnit();
                        final String displayUnit = DisplayValueConversionUtil.getDisplayUnit(recordSet.getType(),
                                recordUnit);

                        final double displayValue = DisplayValueConversionUtil.convertValue(recordSet.getType(),
                                recordUnit,
                                displayUnit, record.getValue());


                        final String displayValueString = DisplayValueConversionUtil.formatDouble(displayValue);

                        label.setValue(displayValueString + " " + displayUnit);
                        label.setDescription(record.getCreated().toString());

                        recordsLayout.addComponent(recordLayout, columnIndex, rowIndex);
                    }
                }
            }
        });
        addExtension(refresher);

    }

    /**
     * Invoked when view is entered.
     * @param parameters the parameters
     */
    public final synchronized void enter(final String parameters) {
        if (recordReaderThread != null) {
            recordReaderExitRequested = true;
            recordReaderThread.interrupt();
            try {
                recordReaderThread.join();
            } catch (final InterruptedException e) {
                LOGGER.debug("Record reader thread death wait interrupted.");
            }
        }
        elementLayout.removeAllComponents();
        recordsLayouts.clear();
        recordsQueue.clear();
        recordReaderExitRequested = false;

        final Company company = siteContext.getObject(Company.class);
        if (company == null || parameters == null || parameters.length() == 0) {
            return;
        }

        final String buildingId = parameters;
        final List<Element> elements = ElementDao.getElements(entityManager, company);

        boolean started = false;
        for (final Element element : elements) {
            if (element.getElementId().equals(buildingId)) {
                started = true;
                continue;
            }
            if (!started) {
                continue;
            }
            if (element.getTreeDepth() == 0) {
                break;
            }

            final HorizontalLayout elementLayout = new HorizontalLayout();
            this.elementLayout.addComponent(elementLayout);
            elementLayout.setSpacing(true);

            final Resource elementIcon;
            switch (element.getType()) {
                case ROOM:
                    elementIcon = roomIcon;
                    break;
                case DEVICE:
                    elementIcon = deviceIcon;
                    break;
                default:
                    elementIcon = deviceIcon;
                    break;
            }

            final Embedded embedded = new Embedded(null, elementIcon);
            elementLayout.addComponent(embedded);
            elementLayout.setExpandRatio(embedded, 0.1f);
            embedded.setWidth(32, Unit.PIXELS);
            embedded.setHeight(32, Unit.PIXELS);

            final Label label = new Label();
            elementLayout.addComponent(label);
            elementLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
            label.setValue(element.toString());

            final GridLayout recordLayout = new GridLayout();
            recordLayout.setSpacing(true);
            recordLayout.setColumns(4);
            recordLayout.setRows(1);

            this.elementLayout.addComponent(recordLayout);
            recordsLayouts.put(element.getElementId(), recordLayout);

        }

        recordReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final EntityManager threadEntityManager = ((EntityManagerFactory) getSite().getSiteContext()
                            .getObject(EntityManagerFactory.class)).createEntityManager();
                    for (final Element element : elements) {
                        final List<RecordSet> recordSets = RecordSetDao.getRecordSets(threadEntityManager, element);
                        if (recordsLayouts.containsKey(element.getElementId())) {
                            for (final RecordSet recordSet : recordSets) {
                                recordsQueue.put(RecordDao.getRecords(threadEntityManager, recordSet, 1));
                                if (recordReaderExitRequested) {
                                    break;
                                }
                            }
                        }
                    }

                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        });
        recordReaderThread.start();

    }

}

