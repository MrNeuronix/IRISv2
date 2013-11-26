package ru.iris.common.script;

import org.mozilla.javascript.ScriptableObject;
import ru.iris.common.Speak;

import javax.jms.JMSException;
import java.net.URISyntaxException;

/**
 * @author Nikolay A. Viguro
 *         Date: 10.09.13
 *         Time: 10:49
 *         To change this template use File | Settings | File Templates.
 */
public class SpeakJS extends ScriptableObject {

    public void jsConstructor() {
    }

    @Override
    public String getClassName() {
        return "SpeakJS";
    }

    public void jsFunction_say(String text) throws JMSException, URISyntaxException {
        new Speak().say(text);
    }
}
