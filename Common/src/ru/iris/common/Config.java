package ru.iris.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: nix
 * Date: 21.10.12
 * Time: 11:44
 * To change this template use File | Settings | File Templates.
 */
public class Config {

    private HashMap<String, String> cfg = new HashMap<String, String>();

    public Config() throws IOException, SQLException {

        Properties prop = new Properties();
        InputStream is = new FileInputStream("./conf/main.property");
        prop.load(is);

        Enumeration em = prop.keys();

        while (em.hasMoreElements()) {
            String key = (String) em.nextElement();
            cfg.put(key, (String) prop.get(key));
        }

        SQL sql = new SQL();

        ResultSet rs = sql.select("SELECT name, param FROM config");

        while (rs.next()) {
            String name = rs.getString("name");
            String val = rs.getString("param");

            cfg.put(name, val);
        }
        rs.close();
    }

    public HashMap<String, String> getConfig() {
        return cfg;
    }
}
