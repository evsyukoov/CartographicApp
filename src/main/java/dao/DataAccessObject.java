package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataAccessObject {
    protected static Connection connection = null;
    private static final String CONNECTION = "jdbc:mysql://127.0.0.1:3306/transform_bot?allowPublicKeyRetrieval=true&useSSL=false" +
            "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String LOGIN = "root";
    private static final String PASS = "1111";

    public synchronized static void register() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("MySQl driver not found");
            ex.printStackTrace();
            System.exit(1);
        }
        System.out.println("MySQl driver registered succesfull");
    }

    //открываем коннекшн

    public synchronized static void startConnection() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION, LOGIN, PASS);
        System.out.println("Connection is established");
    }

    public synchronized static void closeConnection() throws SQLException{
        connection.close();
    }

}
