package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:27
 */

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;

public class SQL {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(SQL.class);

    private Connection connection = null;
    @NonNls
    private static Logger log = LoggerFactory.getLogger(SQL.class.getName());

    public SQL() {

        // Загружаем класс драйвера
        try {
            Class.forName("org.h2.Driver").newInstance();
        } catch (ClassNotFoundException e) {
            log.info("[sql] Error load driver");
            e.printStackTrace();
            System.exit(1);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection("jdbc:h2:tcp://localhost/./conf/iris", "sa", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean doQuery(@NonNls String sql) {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            statement.close();
        } catch (SQLException e1) {
            return false;
        }
        return true;
    }

    public ResultSet select(@NonNls String sql) {
        ResultSet resultSet = null;

        try {
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
