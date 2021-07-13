package Helper;

import dao.DAO;
import dao.DownloadDAO;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

public class ToInlineHelper {

    public static String DOWNLOAD = "INSERT INTO coordinate_systems_inline(id, description, params) VALUES(?,?,?)";

    public static void main(String[] args) throws IOException, SQLException {
        DAO dao = new DownloadDAO();
        dao.startConnection();
        BufferedReader reader = new BufferedReader(new FileReader("transform_bot.csv"));
        PreparedStatement ps = dao.connection.prepareStatement(DOWNLOAD);
        String text;
        int j = 1;
        while ((text = reader.readLine()) != null) {
            if (text.startsWith("\""))
                continue;
            String[] temp = text.split(";");
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].startsWith("\"")) {
                    temp[i] = temp[i].substring(1, temp[i].length() - 1);
                }
            }
            String num = temp[0];
            String type = temp[1];
            String region = temp[2];
            String params = temp[3];
            String zone = temp[4];
            String description = "";
            if (type.equalsIgnoreCase("MSK")) {
                String regionCode = region.substring(0, region.indexOf(" "));
                String regionName = region.substring(region.indexOf(" ") + 1);
                description = type + "-" + regionCode + " " + regionName + " " + zone.toLowerCase();
            }
            else if (type.equalsIgnoreCase("SK-63")) {
                String regionmod = region.replace(region.charAt(0), Character.toLowerCase(region.charAt(0)));
                description = type + " " + regionmod + " " + zone.toLowerCase();
            }
            else if (type.equalsIgnoreCase("Проекты")) {
                description = type + " "  + region + " " + zone.toLowerCase();
            }
            ps.setInt(1, j++);
            ps.setString(2, description);
            ps.setString(3, params);
            ps.executeUpdate();
        }
        reader.close();
        dao.closePrepareStatement(ps);
        System.out.println("Predownload is finish!");

        }
    }
