package dao;
//Возможно нужно будет реализовать догрузку в БД в рантайме,  пока что класс для предзаполнения БД

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

public class DownloadDataAccessObject extends DataAccessObject {
    private static final String DOWNLOAD = "INSERT INTO coordinate_systems(Type, Sk, Param, Zone) VALUES(?,?,?,?)";

    public static void startDownload() throws SQLException {
        startConnection();
        System.out.println("Start download");
        File file = new File("./src/main/java/Helper/Projections.txt");
        Helper helper = new Helper();
        helper.read(file);
        PreparedStatement ps = connection.prepareStatement(DOWNLOAD);
        LinkedList<SystemParam> params = helper.getParams();
        for(int i = 0; i < params.size(); i++)
        {
            ps.setString(1, params.get(i).type);
            ps.setString(2, params.get(i).sk);
            ps.setString(3, params.get(i). params);
            ps.setString(4, params.get(i).zone);
            ps.executeUpdate();
        }
        ps.close();
        System.out.println("Predownload is finish!");
        closeConnection();
    }
}
