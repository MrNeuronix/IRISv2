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

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.addons.sitekit.site.AbstractViewlet;

/**
 * Viewlet which renders image from Theme.
 * @author Tommi S.E. Laukkanen
 */
public final class LargeImageViewlet extends AbstractViewlet {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    @Override
    public void attach() {
        super.attach();
        final String logoImageThemeFileName = getViewletDescriptor().getConfiguration();
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        final Embedded embedded = new Embedded(null, new ThemeResource(logoImageThemeFileName));
        embedded.setWidth(400, UNITS_PIXELS);
        embedded.setHeight(400, UNITS_PIXELS);
        layout.addComponent(embedded);
        layout.setComponentAlignment(embedded, Alignment.MIDDLE_CENTER);
        this.setCompositionRoot(layout);
    }

    /**
     * SiteView constructSite occurred.
     */
    @Override
    public void enter(final String parameters) {

    }

}
