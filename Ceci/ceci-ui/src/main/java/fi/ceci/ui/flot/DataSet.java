package fi.ceci.ui.flot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The data set container for flot sate.
 */
public class DataSet {
    /**
     * The data set label.
     */
    private String label;
    /**
     * The data value pairs.
     */
    private List<List<Object>> data = new ArrayList<List<Object>>();

    /**
     * @return the label
     */
    public final String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public final void setLabel(final String label) {
        this.label = label;
    }

    /**
     * @return the data
     */
    public final List<List<Object>> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public final void setData(final List<List<Object>> data) {
        this.data = data;
    }

    /**
     * Adds date value pair to data.
     * @param date the date
     * @param value the value
     */
    public final void addValue(final Date date, final Double value) {
        data.add(Arrays.asList(new Object[] {date.getTime(), value}));
    }
}
