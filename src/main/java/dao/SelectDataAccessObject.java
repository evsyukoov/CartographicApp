package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class SelectDataAccessObject extends DataAccessObject{

    private static final String SELECT_PARAM = "SELECT params FROM coordinate_systems_inline WHERE description = ?";

    public synchronized static String findCoordinateSystemParam(String description) throws SQLException {
        startConnection();
        String param = null;
        PreparedStatement ps = connection.prepareStatement(SELECT_PARAM);
        ps.setString(1, description);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            param = rs.getString(1);
        }
        rs.close();
        ps.close();
        closeConnection();
        return param;
    }
}
