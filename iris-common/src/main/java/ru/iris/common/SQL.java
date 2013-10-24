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

    private Connection connection = null;
    @NonNls
    private static Logger log = LoggerFactory.getLogger(SQL.class.getName());

    public SQL() throws SQLException, IOException {

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
            log.info("sql] Cannot open connection to database!");
            e.printStackTrace();
            System.exit(1);
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
                e.printStackTrace();
            }
            //statement.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return resultSet;
    }

    public void doDisconnect() throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        statement.close();
    }
}
