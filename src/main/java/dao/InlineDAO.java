package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class InlineDAO extends DAO{
    String receive;

    private static final String QUERY = "SELECT description FROM coordinate_systems_inline WHERE description LIKE ? " +
            "LIMIT 10";

    ArrayList<String> result;

    public void setReceive(String receive) {
        this.receive = receive;
    }

    public ArrayList<String> getResult() {
        return result;
    }

    public void findParams() throws SQLException {
        startConnection();
        result = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(QUERY);
        ps.setString(1, "%" + receive + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            result.add(rs.getString(1));
        }
        result.sort(String::compareTo);
        closePrepareStatement(ps);
        closeConnection();
    }
}
