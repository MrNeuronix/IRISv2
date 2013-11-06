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
package fi.ceci.ui.viewlet;

import com.vaadin.ui.GridLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import fi.ceci.dao.ElementDao;
import fi.ceci.model.Element;
import fi.ceci.model.ElementType;
import fi.ceci.ui.CeciUI;
import fi.ceci.ui.viewlet.dashboard.BuildingControlPanel;
import fi.ceci.ui.viewlet.dashboard.BuildingSelectPanel;
import fi.ceci.ui.viewlet.dashboard.ChartPanel;
import fi.ceci.ui.viewlet.dashboard.EventPanel;
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
public class EventViewlet extends AbstractViewlet {

    /**
     * The event panel.
     */
    private EventPanel eventPanel;

    /**
     * Default constructor which constructs component hierarchy.
     */
    public EventViewlet() {
        if (getSite().getSecurityProvider().getRoles().contains("anonymous")) {
            eventPanel = null;
            setCompositionRoot(new VerticalLayout());
        } else {
            eventPanel = new EventPanel();
            setCompositionRoot(eventPanel);
        }
    }

    @Override
    public final void enter(final String parameters) {
        if (!getSite().getSecurityProvider().getRoles().contains("anonymous") && eventPanel == null) {
            eventPanel = new EventPanel();
            setCompositionRoot(eventPanel);
        }
        if (eventPanel != null) {
            eventPanel.enter(parameters);
        }
    }

}
