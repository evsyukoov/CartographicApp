package bot.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

//Этот класс делает выборку из БД чтобы отдать пользователю
//и получает итоговую строку из БД с помощью которой нужно выполнить пересчет

public class SelectDAO extends DAO{
    private ArrayList<String> types;
    private ArrayList<String> sk;
    private ArrayList<Integer> zones;

    private String param;   //итоговая строка которую ищем

    private String TYPES = "SELECT Type FROM coordinate_systems";
    private String SK = "SELECT Sk FROM coordinate_systems WHERE Type = ?";
    private String ZONE = "SELECT Zone FROM coordinate_systems WHERE Sk = ?";
    private String PARAM = "SELECT Param FROM coordinate_systems WHERE Type = ? AND Sk = ? AND Zone = ?";

    public SelectDAO() {
        types = new ArrayList<String>();
        sk = new ArrayList<String>();
        zones = new ArrayList<Integer>();
    }

    @Override
    public void selectTypes() throws SQLException {
        PreparedStatement ps = connection.prepareStatement(TYPES);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String next = rs.getString(1);
            if (!types.contains(next))
                types.add(next);
        }
        closePrepareStatement(ps);
    }

    @Override
    public void selectSK(String type) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(SK);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String next = rs.getString(1);
            if (!sk.contains(next))
                sk.add(next);
        }
        closePrepareStatement(ps);
    }

    public void selectZone(String sk) throws SQLException
    {
        PreparedStatement ps = connection.prepareStatement(ZONE);
        ps.setString(1, sk);
        ResultSet rs = ps.executeQuery();
        while (rs.next())
        {
            Integer next = rs.getInt(1);
            if (!zones.contains(next))
                zones.add(next);
        }
        closePrepareStatement(ps);
    }

    public void selectParam(String type, String sk, Integer zone) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(PARAM);
        ps.setString(1, type);
        ps.setString(2, sk);
        ps.setInt(3, zone);
        ResultSet rs  = ps.executeQuery();
        rs.next();
        param = rs.getString(1);
        closePrepareStatement(ps);
    }

    public String getParam() {
        return param;
    }
}
