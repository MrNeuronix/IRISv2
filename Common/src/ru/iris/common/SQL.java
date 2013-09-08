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

import java.io.IOException;
import java.sql.*;

public class SQL {

    private Connection connection = null;
    private static Logger log = LoggerFactory.getLogger(SQL.class.getName());

    public SQL() throws SQLException, IOException {

        // Загружаем класс драйвера
        try {
            Class.forName("org.h2.Driver").newInstance();
        } catch (ClassNotFoundException e) {
            log.info("[sql] Error while loading DB driver");
            e.printStackTrace();
            System.exit(1);
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Cоздаем соединение, здесь dbpath это путь к папке где будут хранится
        // файлы БД. dbname имя базы данных. SA это имя пользователя который
        // создается автоматически при создании БД пароль для него пустой. Если
        // такой базы данных нет она будет автоматически создана.

        try {
            connection = DriverManager.getConnection("jdbc:h2:tcp://localhost/./conf/iris", "sa", "");
        } catch (SQLException e) {
            log.info("[sql] Cant open connection to H2 database!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Выполнения произвольного запроса

    public boolean doQuery(String sql) {
        try {
            Statement statement = connection.createStatement();
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
                // если таблица создана, будет исключение, игнорируем его.
                //в реальных проектах так не делают
            }
            statement.close();
        } catch (SQLException e1) {
            return false;
        }
        return true;
    }

    // Метод вытаскивания данных

    public ResultSet select(String sql) {
        ResultSet resultSet = null;

        try {
            Statement statement = connection.createStatement();
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

    // Метод отключения от БД

    public void doDisconnect() throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        statement.close();
    }
}
