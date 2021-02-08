package dao;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//заполняет клиентскую БД тем что выбрал клиент
public class ClientDAO extends DAO
{
    long clientId;

    private final String TYPE = "UPDATE clients SET Using = ? WHERE id = ?";
    private final String SK = "UPDATE clients SET Sk = ? WHERE id = ?";
    private final String STATE = "UPDATE clients SET State = ? WHERE id = ?";
    private final String SELECT = "SELECT Type, Sk FROM clients WHERE id = ?";
    private final String ID = "SELECT id FROM clients WHERE id = ?";
    private final String ADD = "INSERT INTO clients VALUES(?,?)";
    private final String GETUSING = "SELECT state FROM clients WHERE id = ?";
    private final String USING = "UPDATE clients SET count=count+1 WHERE id=?";
    private final String TRY = "SELECT * FROM clients WHERE id = ?";


    // то что выбрал клиент, забираем из БД
    private String choosedSK;
    private String choosedType;

    public ClientDAO(long id) {
        this.clientId = id;
    }


    public void addToDataBase() throws SQLException {
        PreparedStatement ps = connection.prepareStatement(ID);
        ps.setLong(1, clientId);
        ResultSet rs = ps.executeQuery();
        //добавляем
        if (!rs.next())
        {
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

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public  void setType(String type) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(TYPE);
        refreshBD(ps, type);
    }

    public void setSK(String sk) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(SK);
        refreshBD(ps, sk);
    }

    public void setState(int state) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(STATE);
        ps.setInt(1, state);
        ps.setLong(2, clientId);
        ps.executeUpdate();
        closePrepareStatement(ps);
    }

    public boolean addNewClient() throws SQLException
    {
        setClientId(clientId);

        PreparedStatement ps = connection.prepareStatement(ID);
        ps.setLong(1, clientId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next())
        {
            PreparedStatement add = connection.prepareStatement(USING);
            add.setLong(1, clientId);
            add.executeUpdate();
            closePrepareStatement(add);
            setState(0);
            return false;
        }
        else
            return true;
    }

    private void refreshBD(PreparedStatement ps, String arg) throws SQLException
    {
        ps.setString(1, arg);
        ps.setLong(2, clientId);
        ps.executeUpdate();
        closePrepareStatement(ps);
    }

    public int  getClientUsing() throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(GETUSING);
        ps.setLong(1, clientId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public void  getData() throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(SELECT);
        ps.setLong(1, clientId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        choosedType = rs.getString(1);
        choosedSK = rs.getString(2);
    }

    public String getChoosedSK() {
        return choosedSK;
    }

    public String getChoosedType() {
        return choosedType;
    }
}
