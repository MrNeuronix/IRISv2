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

import com.vaadin.ui.GridLayout;
import com.vaadin.ui.UI;
import fi.ceci.dao.ElementDao;
import fi.ceci.model.Element;
import fi.ceci.model.ElementType;
import fi.ceci.ui.CeciUI;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * ago control site dashboard.
 *
 * @author  Tommi S.E. Laukkanen
 */
public class DashboardViewlet extends AbstractViewlet {

    /**
     * The building select panel.
     */
    private final BuildingSelectPanel buildingSelectPanel;
    /**
     * The building control panel.
     */
    private final BuildingControlPanel buildingControlPanel;
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
     * The event panel.
     */
    private final EventPanel eventPanel;
    /**
     * The chart panel.
     */
    private final ChartPanel chartPanel;

    /**
     * Default constructor which constructs component hierarchy.
     */
    public DashboardViewlet() {
        site = ((CeciUI) UI.getCurrent()).getSite();
        siteContext = getSite().getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        final GridLayout gridLayout = new GridLayout();
        gridLayout.setSizeFull();
        gridLayout.setRows(3);
        gridLayout.setColumns(2);
        gridLayout.setSpacing(true);
        gridLayout.setColumnExpandRatio(0, 1);
        gridLayout.setColumnExpandRatio(1, 0);
        gridLayout.setRowExpandRatio(0, 0);
        gridLayout.setRowExpandRatio(1, 0);
        gridLayout.setRowExpandRatio(2, 1);


        buildingSelectPanel = new BuildingSelectPanel();
        //buildingSelectPanel.setCaption("Building Selection");
        //buildingSelectPanel.setSizeFull();
        gridLayout.addComponent(buildingSelectPanel, 0, 0, 1, 0);

        buildingControlPanel = new BuildingControlPanel();
        //buildingControlPanel.setCaption("Control Panel");
        //buildingControlPanel.setHeight(200, Unit.PIXELS);
        buildingControlPanel.setSizeFull();
        gridLayout.addComponent(buildingControlPanel, 0, 1, 0, 2);

        chartPanel = new ChartPanel();
        chartPanel.setSizeFull();
        chartPanel.setWidth(700, Unit.PIXELS);
        chartPanel.setHeight(400, Unit.PIXELS);
        gridLayout.addComponent(chartPanel, 1, 1);

        eventPanel = new EventPanel();
        //eventPanel.setCaption("Bus Events");
        //ventPanel.setHeight(200, Unit.PIXELS);
        eventPanel.setSizeFull();
        gridLayout.addComponent(eventPanel, 1, 2);

        setCompositionRoot(gridLayout);
    }

    @Override
    public final void enter(final String parameters) {
        final Company company = siteContext.getObject(Company.class);

        final List<Element> buildings = ElementDao.getElements(entityManager, company, ElementType.BUILDING);
        if (parameters.length() == 0 && buildings.size() > 0) {
            UI.getCurrent().getNavigator().navigateTo("default/" + buildings.get(0).getElementId());
        } else {
            buildingSelectPanel.enter(parameters);
            buildingControlPanel.enter(parameters);
            chartPanel.enter(parameters);
            eventPanel.enter(parameters);
        }
    }

}
