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
package fi.ceci.ui.viewlet.bus;

import com.vaadin.ui.Select;
import fi.ceci.model.BusConnectionStatus;

/**
 * BusConnectionStatus field.
 *
 * @author Tommi Laukkanen
 */
public class BusConnectionStatusField extends Select {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor which populates the select with existing customers.
     */
    public BusConnectionStatusField() {
        super();
    }

    @Override
    public final void attach() {
        super.attach();

        for (final BusConnectionStatus type : BusConnectionStatus.values()) {
            addItem(type);
        }
    }

}
