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

import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.BaseTheme;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.NavigationVersion;
import org.vaadin.addons.sitekit.site.ViewVersion;

import java.util.List;

/**
 * Default navigation Viewlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class NavigationViewlet extends AbstractViewlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    private List<String> lastRoles;

    @Override
    public void attach() {
        super.attach();
    }

    public void refresh() {

        final HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setMargin(true);
        mainLayout.setSizeFull();

        final VerticalLayout navigationLayout = new VerticalLayout();
        navigationLayout.setWidth(100, Unit.PIXELS);
        navigationLayout.setSpacing(true);
        navigationLayout.setMargin(false);
        mainLayout.addComponent(navigationLayout);
        mainLayout.setComponentAlignment(navigationLayout, Alignment.TOP_CENTER);

        final NavigationVersion navigationVersion = getSite().getCurrentNavigationVersion();

        //final ViewDescriptor page = getPageWindow().getViewDescriptor();
        final String[] elements = navigationVersion.getTree().split(";");
        for (final String element : elements) {
            final int indentLastIndex = element.lastIndexOf('+');
            final String pageName = indentLastIndex == -1 ? element : element.substring(indentLastIndex + 1);

            final ViewVersion pageVersion = getSite().getCurrentViewVersion(pageName);
            if (pageVersion.getViewerRoles().length > 0) {
                boolean roleMatch = false;
                for (final String role : pageVersion.getViewerRoles()) {
                    if (getSite().getSecurityProvider().getRoles().contains(role)) {
                        roleMatch = true;
                        break;
                    }
                }
                if (!roleMatch) {
                    continue;
                }
            }

            final String localizedPageName = getSite().localize("page-link-" + pageName);
            final Resource iconResource = getSite().getIcon("page-icon-" + pageName);

            final HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setSpacing(true);
            itemLayout.setMargin(false);
            navigationLayout.addComponent(itemLayout);

            final int indentCount = indentLastIndex + 1;
            final StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < indentCount; i++) {
                indentBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }

            final Label indent = new Label(indentBuilder.toString(), Label.CONTENT_XHTML);
            itemLayout.addComponent(indent);
            if (iconResource != null) {
                final Embedded embedded = new Embedded(null, iconResource);
                embedded.setWidth(32, UNITS_PIXELS);
                embedded.setHeight(32, UNITS_PIXELS);
                itemLayout.addComponent(embedded);
            }

            final Button link = new Button(localizedPageName);
            link.setStyleName(BaseTheme.BUTTON_LINK);
            link.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    UI.getCurrent().getNavigator().navigateTo(pageName);
                }
            });
            /*if (page.getName().equals(pageName)) {
                link.setEnabled(false);
            }*/

            itemLayout.addComponent(link);
            itemLayout.setComponentAlignment(link, Alignment.MIDDLE_LEFT);
        }

        if (getSite().getSecurityProvider().getUser() != null) {
            final HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setSpacing(true);
            itemLayout.setMargin(false);
            navigationLayout.addComponent(itemLayout);

            final int indentCount = 0;
            final StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < indentCount; i++) {
                indentBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }

            final Resource iconResource = getSite().getIcon("page-icon-logout");
            final Label indent = new Label(indentBuilder.toString(), Label.CONTENT_XHTML);
            itemLayout.addComponent(indent);
            if (iconResource != null) {
                final Embedded embedded = new Embedded(null, iconResource);
                embedded.setWidth(32, UNITS_PIXELS);
                embedded.setHeight(32, UNITS_PIXELS);
                itemLayout.addComponent(embedded);
            }

            final Button logoutButton = new Button(getSite().localize("button-logout"));
            logoutButton.setStyleName(BaseTheme.BUTTON_LINK);
            logoutButton.addClickListener(new ClickListener() {
                /** Serial version UID. */
                private static final long serialVersionUID = 1L;

                @Override
                public void buttonClick(final ClickEvent event) {
                    final Company company = getSite().getSiteContext().getObject(Company.class);
                    getUI().getPage().setLocation(company.getUrl());

                    // Close the VaadinSession
                    getSession().close();

                }
            });

            itemLayout.addComponent(logoutButton);
            itemLayout.setComponentAlignment(logoutButton, Alignment.MIDDLE_LEFT);
        }

        setCompositionRoot(mainLayout);
    }

    /**
     * SiteView constructSite occurred.
     */
    @Override
    public void enter(final String parameters) {
        final List<String> currentRoles = getSite().getSecurityProvider().getRoles();
        if (!currentRoles.equals(lastRoles)) {
            refresh();
            lastRoles = currentRoles;
        }
    }
}
