package dao;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClientDAO extends DAO {
    long clientId;
    String name;
    String nickname;

    private final String ID = "SELECT id FROM clients WHERE id = ?";
    private final String ADD = "INSERT INTO clients VALUES(?,?,?,?)";
    private final String USING = "UPDATE clients SET count=count+1 WHERE id=?";

    private final String SELECT_ALL = "SELECT id FROM clients";



    public ClientDAO() {
    }

    public ClientDAO(long id, String firstName, String lastName, String nickname) {
        this.clientId = id;
        if (nickname == null)
            this.nickname = "None";
        else
            this.nickname = nickname;
        if (firstName == null && lastName == null)
            name = "None";
        else if (firstName != null && lastName == null)
            this.name = firstName;
        else if (firstName == null && lastName != null)
            this.name = lastName;
        else
            this.name = String.format("%s %s", firstName, lastName);
    }

    public List<Long> getAllClients() throws SQLException {
        List<Long> allID = new LinkedList<>();
        PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
        ResultSet rs = ps.executeQuery();
        while (rs.next())
            allID.add(rs.getLong(1));
        closePrepareStatement(ps);
        rs.close();
        return allID;
    }

    public void addToDataBase() throws SQLException {
        PreparedStatement ps = connection.prepareStatement(ID);
        ps.setLong(1, clientId);
        ResultSet rs = ps.executeQuery();
        //добавляем
        if (!rs.next()) {
            PreparedStatement insert = connection.prepareStatement(ADD);
            insert.setLong(1, clientId);
            insert.setInt(2, 0);
            insert.setString(3, name);
            insert.setString(4, nickname);
            insert.executeUpdate();
            insert.close();

        }
        //обновляем счетчик заходов
        else {
            PreparedStatement refresh = connection.prepareStatement(USING);
            refresh.setLong(1, clientId);
            refresh.executeUpdate();
            refresh.close();
        }
        rs.close();
        ps.close();
    }
}


