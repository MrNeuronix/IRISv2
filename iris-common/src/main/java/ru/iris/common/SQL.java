package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:27
 */


import com.sun.rowset.CachedRowSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SQL {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(SQL.class);
    private Connection connection;
    private final Config config = new Config();

    public SQL() {
        connect();
    }

    private void connect() {
        try {
            String driverName = "com.mysql.jdbc.Driver";

            Class.forName(driverName);

            // Create a connection to the database
            String serverName = config.getConfig().get("dbHost");
            String mydatabase = config.getConfig().get("dbName") + "?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true";
            String connectionURL = "jdbc:mysql://" + serverName + "/" + mydatabase;
            String username = config.getConfig().get("dbUsername");
            String password = config.getConfig().get("dbPassword");
            connection = DriverManager.getConnection(connectionURL, username, password);

        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.error("[sql] Error load driver: " + e.toString());
        }
    }

    public boolean doQuery(String sql) {

        try {
            if (!connection.isValid(1))
                connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Statement statement = null;

        try {
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            try {
                statement.executeUpdate(sql);
            } catch (Exception e) {
                LOGGER.error("SQL error: " + e);
                e.printStackTrace();
            }
        } catch (Exception e1) {
            LOGGER.error("SQL error: " + e1);
            return false;
        } finally {
            //closing the resources in this transaction
            //similar logic than the used in the last close block code
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
            }
        }
        return true;
    }

    public ResultSet select(String sql) {

        try {
            if (!connection.isValid(1))
                connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ResultSet resultSet = null;
        Statement statement = null;
        CachedRowSetImpl crs = null;

        try {
            crs = new CachedRowSetImpl();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            try {
                resultSet = statement.executeQuery(sql);
                crs.populate(resultSet);
            } catch (SQLException e) {
                LOGGER.error("Error executing: " + sql + ": " + e.getMessage());
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        } finally {
            //closing the resources in this transaction
            //similar logic than the used in the last close block code
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
            }
        }

        return crs;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
