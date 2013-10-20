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
package fi.ceci.ui.flot;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;

/**
 * Flot chart component.
 *
 * @author Tommi S.E. Laukkanen
 */
@JavaScript({"flotr2.min.js", "flot.js"})
public class Flot extends AbstractJavaScriptComponent {
    /**
     * The default constructor.
     */
    public Flot() {
        setSizeFull();
    }

    /**
     * Gets the flot state.
     * @return the flot state
     */
    public final FlotState getState() {
        return (FlotState) super.getState(true);
    }
}
