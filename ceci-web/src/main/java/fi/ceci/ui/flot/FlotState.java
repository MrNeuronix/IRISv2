package fi.ceci.ui.flot;

import com.vaadin.shared.ui.JavaScriptComponentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The flot state.
 *
 * @author Tommi S.E. Laukkanen
 */
public class FlotState extends JavaScriptComponentState {
    /** The graph options. */
    private Map<String, Map<String, Object>> options = new HashMap<String, Map<String, Object>>();

    /** The data sets. */
    private List<DataSet> dataSets = new ArrayList<DataSet>();

    /**
     * @param key the option section key
     * @return the options section
     */
    public final Map<String, Object> getOptions(final String key) {
        if (!options.containsKey(key)) {
            options.put(key, new HashMap<String, Object>());
        }
        return options.get(key);
    }

    /**
     * @return the options
     */
    public final Map<String, Map<String, Object>> getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public final void setOptions(final Map<String, Map<String, Object>> options) {
        this.options = options;
    }

    /**
     * @return the dataSets
     */
    public final List<DataSet> getDataSets() {
        return dataSets;
    }

    /**
     * @param dataSets the dataSets to set
     */
    public final void setDataSets(final List<DataSet> dataSets) {
        this.dataSets = dataSets;
    }
}
