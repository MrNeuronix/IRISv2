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

import com.vaadin.data.Property;
import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import fi.ceci.dao.ElementDao;
import fi.ceci.dao.RecordDao;
import fi.ceci.dao.RecordSetDao;
import fi.ceci.model.*;
import fi.ceci.ui.CeciUI;
import fi.ceci.ui.flot.DataSet;
import fi.ceci.ui.flot.Flot;
import fi.ceci.ui.flot.FlotState;
import fi.ceci.util.DisplayValueConversionUtil;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * The building control panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class ChartPanel extends AbstractViewlet {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(ChartPanel.class);

    /**
     * The layout.
     */
    private final VerticalLayout layout;
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
     * The rooms layout.
     */
    private final VerticalLayout chartLayout;
    /**
     * The building ID.
     */
    private String buildingId;
    private final ComboBox recordTypeComboBox;
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
    private final Label title;


    /**
     * Default constructor.
     */
    public ChartPanel() {
        site = ((CeciUI) UI.getCurrent()).getSite();
        siteContext = site.getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        temperatureIcon = site.getIcon("temperature");
        brightnessIcon = site.getIcon("brightness");
        humidityIcon = site.getIcon("humidity");
        eventIcon = site.getIcon("event");

        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.setStyleName(Reindeer.LAYOUT_WHITE);

        title = new Label("Record Chart");
        title.setImmediate(true);
        title.setIcon(getSite().getIcon("folder"));
        title.setStyleName(Reindeer.LABEL_H2);
        layout.addComponent(title);
        layout.setExpandRatio(title, 0);

        recordTypeComboBox = new ComboBox();
        layout.addComponent(recordTypeComboBox);
        layout.setComponentAlignment(recordTypeComboBox, Alignment.MIDDLE_LEFT);
        layout.setExpandRatio(recordTypeComboBox, 0);
        //buildingComboBox.setWidth(100, Unit.PERCENTAGE);
        recordTypeComboBox.setNullSelectionAllowed(false);
        recordTypeComboBox.setNewItemsAllowed(false);
        recordTypeComboBox.setTextInputAllowed(false);
        recordTypeComboBox.setImmediate(true);
        recordTypeComboBox.setBuffered(false);

        for (final RecordType recordType : RecordType.values()) {
            recordTypeComboBox.addItem(recordType);
        }
        recordTypeComboBox.setValue(RecordType.TEMPERATURE);
        recordTypeComboBox.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(final Property.ValueChangeEvent event) {
                refresh();
            }
        });

        chartLayout = new VerticalLayout();
        layout.addComponent(chartLayout);
        layout.setExpandRatio(chartLayout, 1);
        chartLayout.setSpacing(true);
        chartLayout.setSizeFull();

        setCompositionRoot(layout);
    }

    /**
     * Invoked when view is entered.
     * @param parameters the parameters
     */
    public final void enter(final String parameters) {
        buildingId = parameters;
        refresh();
    }

    /**
     * Refreshes chart.
     */
    private void refresh() {
        final Company company = siteContext.getObject(Company.class);
        if (company == null || buildingId == null) {
            return;
        }

        final Element building = ElementDao.getElement(entityManager, buildingId);

        final RecordType recordType = (RecordType) recordTypeComboBox.getValue();

        final Resource icon;
        switch (recordType) {
            case TEMPERATURE:
                icon = temperatureIcon;
                break;
            case BRIGHTNESS:
                icon = brightnessIcon;
                break;
            case HUMIDITY:
                icon = humidityIcon;
                break;
            default:
                icon = eventIcon;
                break;
        }
        title.setIcon(icon);


        final List<RecordSet> recordSets = new ArrayList<RecordSet>();

        final List<Element> elements = ElementDao.getElements(entityManager, company);
        final Map<String, Element> elementMap = new HashMap<String, Element>();

        boolean started = false;
        for (final Element element : elements) {
            elementMap.put(element.getElementId(), element);
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
            if (element.getType() == ElementType.DEVICE) {
                continue;
            }

            recordSets.addAll(RecordSetDao.getRecordSetsByParent(
                    entityManager, element, recordType));
        }

        chartLayout.removeAllComponents();

        if (recordSets.size() == 0) {
            return;
        }

        final Flot flot = new Flot();

        final String recordUnit = recordSets.get(0).getUnit();
        final String displayUnit = DisplayValueConversionUtil.getDisplayUnit(recordSets.get(0).getType(), recordUnit);

        final FlotState state = flot.getState();
        state.getOptions("options").put("HtmlText", false);
        state.getOptions("options").put("title", recordType.toString() + "[" + displayUnit + "]");
        state.getOptions("selection").put("mode", "x");
        state.getOptions("xaxis").put("mode", "time");
        state.getOptions("xaxis").put("labelsAngle", Double.valueOf(45));

        final Date since = new Date(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L);
        for (final RecordSet recordSet : recordSets) {
            final DataSet dataSet = new DataSet();
            final Element element = recordSet.getElement();
            final Element parentElement = elementMap.get(element.getParent().getElementId());
            dataSet.setLabel(parentElement.getName() + " / " + element.getName());

            final List<Record> records = RecordDao.getRecords(entityManager, recordSet, since);
            for (final Record record : records) {
                final double displayValue = DisplayValueConversionUtil.convertValue(recordSet.getType(), recordUnit,
                        displayUnit, record.getValue());
                        dataSet.addValue(record.getCreated(), displayValue);
            }

            state.getDataSets().add(dataSet);
        }

        flot.setHeight(280, Unit.PIXELS);
        //setWidth(400, Unit.PIXELS);
        //setHeight(300, Unit.PIXELS);
        chartLayout.addComponent(flot);

    }

}
