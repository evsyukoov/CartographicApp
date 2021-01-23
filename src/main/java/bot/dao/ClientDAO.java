package bot.dao;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//заполняет клиентскую БД тем что выбрал клиент
public class ClientDAO extends DAO
{
    int clientId;

    private final String TYPE = "UPDATE clients SET Type = ? WHERE id = ?";
    private final String SK = "UPDATE clients SET Sk = ? WHERE id = ?";
    private final String STATE = "UPDATE clients SET State = ? WHERE id = ?";
    private final String SELECT = "SELECT Type, Sk FROM clients WHERE id = ?";

    // то что выбрал клиент, забираем из БД
    private String choosedSK;
    private String choosedType;

    public ClientDAO(int clientId) {
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
        ps.setInt(2, clientId);
        ps.executeUpdate();
        closePrepareStatement(ps);
    }

    private void refreshBD(PreparedStatement ps, String arg) throws SQLException
    {
        ps.setString(1, arg);
        ps.setInt(2, clientId);
        ps.executeUpdate();
        closePrepareStatement(ps);
    }

    public void  getData() throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(SELECT);
        ps.setInt(1, clientId);
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
