package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:27
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SQL {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(SQL.class);
    private Connection connection;

    public SQL() {

        // Загружаем класс драйвера
        try {

            String driverName = "com.mysql.jdbc.Driver";

            Class.forName(driverName);

            // Create a connection to the database
            Config config = new Config();
            String serverName = config.getConfig().get("dbHost");
            String mydatabase = config.getConfig().get("dbName")+"?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=UTF-8";
            String url = "jdbc:mysql://" + serverName + "/" + mydatabase;
            String username = config.getConfig().get("dbUsername");
            String password = config.getConfig().get("dbPassword");

            connection = DriverManager.getConnection(url, username, password);
            LOGGER.info("Connection to database established: " + connection);

        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.info("[sql] Error load driver");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean doQuery(String sql) {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            try {
                statement.executeUpdate(sql);
            } catch (Exception e) {
                LOGGER.info("SQL error: "+e);
                e.printStackTrace();
            }
            statement.close();
        } catch (Exception e1) {
            LOGGER.info("SQL error: "+e1);
            return false;
        }
        return true;
    }

    public ResultSet select( String sql) {

        ResultSet resultSet = null;

        try {
            //TODO Need to close statement!
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            try {
                resultSet = statement.executeQuery(sql);
            } catch (SQLException e) {
                LOGGER.warn("Error executing: " + sql + ": " + e.getMessage());
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        return resultSet;
    }

    public void close() throws SQLException {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
