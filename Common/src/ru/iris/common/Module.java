package ru.iris.common;

import javax.jms.JMSException;

/**
 * Created with IntelliJ IDEA.
 * User: nix
 * Date: 21.10.12
 * Time: 2:53
 * To change this template use File | Settings | File Templates.
 */
public interface Module {
    public void run(String arg) throws JMSException;
}