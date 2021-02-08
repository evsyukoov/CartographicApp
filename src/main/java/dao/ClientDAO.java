package dao;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDAO extends DAO {
    long clientId;

    private final String ID = "SELECT id FROM clients WHERE id = ?";
    private final String ADD = "INSERT INTO clients VALUES(?,?)";
    private final String USING = "UPDATE clients SET count=count+1 WHERE id=?";


    public ClientDAO(long id) {
        this.clientId = id;
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


