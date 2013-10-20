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

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.GridLayout;
import org.vaadin.addons.sitekit.site.AbstractSiteView;

/**
 * Fixed width implementation of PageWindow.
 * @author Tommi S.E. Laukkanen
 */
public final class WideView extends AbstractSiteView {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The navigation column width. */
    private static final int NAVIGATION_COLUMN_WIDTH = 150;
    /** The navigation slot height. */
    private static final int NAVIGATION_HEIGHT = 600;

    /**
     * The default portal window.
     */
    public WideView() {
        super();
        setImmediate(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeComponents() {
        final int columnCount = 5;
        final int rowCount = 3;

        final GridLayout layout = this;
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setColumns(columnCount);
        layout.setRows(rowCount);
        //layout.setColumnExpandRatio(0, MARGIN_COLUMN_EXPAND_RATIO);
        //layout.setColumnExpandRatio(MARGIN_COLUMN_RIGTH_INDEX, MARGIN_COLUMN_EXPAND_RATIO);
        layout.setRowExpandRatio(1, 1.0f);
        layout.setColumnExpandRatio(2, 1.0f);
        layout.setSizeFull();

        final AbstractComponent logoComponent = getComponent("logo");
        logoComponent.setWidth(NAVIGATION_COLUMN_WIDTH, Unit.PIXELS);
        layout.addComponent(logoComponent, 1, 0);

        final AbstractComponent navigationComponent = getComponent("navigation");
        navigationComponent.setWidth(NAVIGATION_COLUMN_WIDTH, Unit.PIXELS);
        navigationComponent.setHeight(NAVIGATION_HEIGHT, Unit.PIXELS);
        layout.addComponent(navigationComponent, 1, 1);

        //final AbstractComponent headerComponent = getComponent("header");
        //headerComponent.setWidth(CONTENT_COLUMN_WIDTH, Unit.PIXELS);
        //headerComponent.setSizeFull();
        //layout.addComponent(headerComponent, 2, 0);

        final AbstractComponent contentComponent = getComponent("content");
        //contentComponent.setWidth(CONTENT_COLUMN_WIDTH, Unit.PIXELS);
        contentComponent.setSizeFull();
        layout.addComponent(contentComponent, 2, 0, 2, 2);

        //final AbstractComponent footerComponent = getComponent("footer");
        //headerComponent.setWidth(CONTENT_COLUMN_WIDTH, Unit.PIXELS);
        //layout.addComponent(footerComponent, 2, 2);
    }


}
