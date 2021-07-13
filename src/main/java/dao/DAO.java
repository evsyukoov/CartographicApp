package dao;

import Helper.SystemParam;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DAO {
    public Connection connection = null;
    private final String CONNECTION = "jdbc:mysql://127.0.0.1:3306/transform_bot?allowPublicKeyRetrieval=true&useSSL=false" +
            "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private final String LOGIN = "root";
    private final String PASS = "1111";

    public static void closePrepareStatement(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void register() {
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

    public void startConnection() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION, LOGIN, PASS);
        System.out.println("Connection is established");
    }

    public void closeConnection() throws SQLException{
        connection.close();
    }

}
