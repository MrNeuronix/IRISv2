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

import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;
import fi.ceci.ui.CeciFields;
import fi.ceci.ui.CeciUI;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import org.vaadin.addons.sitekit.grid.FieldDescriptor;
import org.vaadin.addons.sitekit.grid.FilterDescriptor;
import org.vaadin.addons.sitekit.grid.FormattingTable;
import org.vaadin.addons.sitekit.grid.Grid;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.util.ContainerUtil;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Event panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class EventPanel extends AbstractViewlet {
    /** The container. */
    private LazyEntityContainer<fi.ceci.model.Event> container;
    /** The grid. */
    private Grid grid;

    /**
     * Default constructor which constructs components.
     */
    public EventPanel() {
        final List<FieldDescriptor> fieldDescriptors = CeciFields.getFieldDescriptors(fi.ceci.model.Event.class);
        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();

        final Site site = ((CeciUI) UI.getCurrent()).getSite();
        final EntityManager entityManager = site.getSiteContext().getObject(EntityManager.class);
        container = new LazyEntityContainer<fi.ceci.model.Event>(entityManager, true, false, false,
                fi.ceci.model.Event.class, 50,
                new String[] {"created"},
                new boolean[] {false}, "eventId");

        ContainerUtil.addContainerProperties(container, fieldDescriptors);

        final Table table = new FormattingTable();
        grid = new Grid(table, container, false);
        setCompositionRoot(grid);

        grid.setFields(fieldDescriptors);
        grid.setFilters(filterDefinitions);
        table.setHeight(400, Unit.PIXELS);
        //grid.setSizeUndefined();
        //grid.setSizeUndefined();

        table.setColumnCollapsed("eventId", true);
        table.setColumnCollapsed("modified", true);
        table.setColumnCollapsed("processed", true);
        table.setColumnCollapsed("processingError", true);

        final Company company = site.getSiteContext().getObject(Company.class);
        container.removeDefaultFilters();
        container.addDefaultFilter(
                new Compare.Equal("owner.companyId", company.getCompanyId()));
    }

    /**
     * @param parameters view parameters
     */
    public final void enter(final String parameters) {
        grid.refresh();
    }
}
