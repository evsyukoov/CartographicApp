package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDataAccessObject extends DataAccessObject {
    private static final String ID = "SELECT id FROM clients WHERE id = ?";
    private static final String ADD = "INSERT INTO clients VALUES(?,?,?,?)";
    private static final String USING = "UPDATE clients SET count=count+1 WHERE id=?";


    public synchronized static void addToDataBase(long id, String firstName, String lastName, String nickname) throws SQLException {
        String name;
        if (nickname == null)
            nickname = "None";
        if (firstName == null && lastName == null)
            name = "None";
        else if (firstName != null && lastName == null)
            name = firstName;
        else if (firstName == null && lastName != null)
            name = lastName;
        else
            name = String.format("%s %s", firstName, lastName);

        startConnection();
        PreparedStatement ps = connection.prepareStatement(ID);
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            PreparedStatement insert = connection.prepareStatement(ADD);
            insert.setLong(1, id);
            insert.setInt(2, 0);
            insert.setString(3, name);
            insert.setString(4, nickname);
            insert.executeUpdate();
            insert.close();

        } else {
            PreparedStatement refresh = connection.prepareStatement(USING);
            refresh.setLong(1, id);
            refresh.executeUpdate();
            refresh.close();
        }
        rs.close();
        ps.close();
        closeConnection();
    }
}


