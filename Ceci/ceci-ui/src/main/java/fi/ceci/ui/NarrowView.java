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
public final class NarrowView extends AbstractSiteView {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The margin column expand ratio. */
    private static final float MARGIN_COLUMN_EXPAND_RATIO = 0.5f;
    /** Index of the rigth margin column. */
    private static final int MARGIN_COLUMN_RIGTH_INDEX = 4;
    /** The content column width. */
    private static final int CONTENT_COLUMN_WIDTH = 400;

    /**
     * The default portal window.
     */
    public NarrowView() {
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
        layout.setColumns(columnCount);
        layout.setRows(rowCount);
        layout.setColumnExpandRatio(0, MARGIN_COLUMN_EXPAND_RATIO);
        layout.setColumnExpandRatio(MARGIN_COLUMN_RIGTH_INDEX, MARGIN_COLUMN_EXPAND_RATIO);
        layout.setRowExpandRatio(1, 1.0f);
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(true);

        final AbstractComponent headerComponent = getComponent("logo");
        headerComponent.setWidth(CONTENT_COLUMN_WIDTH, Unit.PIXELS);
        layout.addComponent(headerComponent, 2, 0);

        final AbstractComponent contentComponent = getComponent("content");
        contentComponent.setWidth(CONTENT_COLUMN_WIDTH, Unit.PIXELS);
        contentComponent.setSizeFull();
        layout.addComponent(contentComponent, 2, 1);

    }


}
