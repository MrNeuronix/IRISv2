package fi.ceci.ui.viewlet;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.SiteContext;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tommi
 * Date: 10/20/13
 * Time: 9:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusViewlet extends AbstractViewlet {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    private HorizontalLayout layout;

    @Override
    public void attach() {
        super.attach();
        final VerticalLayout mainLayout = new VerticalLayout();
        layout = new HorizontalLayout();
        layout.setMargin(false);
        layout.setSpacing(true);
        mainLayout.addComponent(layout);
        mainLayout.setComponentAlignment(layout, Alignment.TOP_CENTER);

        this.setCompositionRoot(mainLayout);


    }

    /**
     * SiteView constructSite occurred.
     */
    @Override
    public void enter(final String parameters) {
        layout.removeAllComponents();

        final SiteContext siteContext = getSite().getSiteContext();
        final Map<String, Boolean> statuses = siteContext.getObject("statuses");
        statuses.put("authentication", getSite().getSecurityProvider().getRoles().indexOf("anonymous") == -1);

        for (final String statusKey : statuses.keySet())  {
            final Embedded embedded = new Embedded(null, new ThemeResource(statusKey + ".png"));
            embedded.setWidth(48, Unit.PIXELS);
            embedded.setHeight(48, Unit.PIXELS);
            embedded.setEnabled(statuses.get(statusKey));
            embedded.setImmediate(true);
            layout.addComponent(embedded);
            layout.setComponentAlignment(embedded, Alignment.MIDDLE_CENTER);
        }
    }

}