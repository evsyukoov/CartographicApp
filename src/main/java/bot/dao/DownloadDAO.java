package bot.dao;


//Возможно нужно будет реализовать догрузку в БД в рантайме,  пока что класс для предзаполнения БД

import Helper.Helper;
import Helper.SystemParam;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

public class DownloadDAO extends DAO {
    private final String DOWNLOAD = "INSERT INTO coordinate_systems(Type, Sk, Param, Zone) VALUES(?,?,?,?)";

    public void startDownload() throws SQLException {
        File file = new File("./projections.txt");
        Helper helper = new Helper();
        helper.read(file);
        PreparedStatement ps = connection.prepareStatement(DOWNLOAD);
        LinkedList<SystemParam> params = helper.getParams();
        for(int i = 0; i < params.size(); i++)
        {
            ps.setString(1, params.get(i).type);
            ps.setString(2, params.get(i).sk);
            ps.setString(3, params.get(i). params);
            ps.setInt(4, params.get(i).zone);
            ps.executeUpdate();
        }
        closePrepareStatement(ps);

    }
}
