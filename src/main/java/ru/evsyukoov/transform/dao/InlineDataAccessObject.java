package ru.evsyukoov.transform.dao;

import com.ibm.icu.text.Transliterator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class InlineDataAccessObject extends DataAccessObject{
    String receive;

    private static final String QUERY = "SELECT description FROM coordinate_systems_inline WHERE description LIKE ? " +
            "LIMIT 10";

    public synchronized static ArrayList<String> findParams(String receive) throws SQLException {
        startConnection();
        PreparedStatement ps = connection.prepareStatement(QUERY);
        ArrayList<String> result = new ArrayList<>(10);
        innerFind(ps, receive, result);
        if (result.isEmpty()) {
            receive = transliteration(receive);
            innerFind(ps, receive, result);
        }
        result.sort(String::compareTo);
        ps.close();
        closeConnection();
        return result;
    }

    private static void    innerFind(PreparedStatement ps, String receive, ArrayList<String> result) throws SQLException {
        ps.setString(1, "%" + receive + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            result.add(rs.getString(1));
        }
    }

    private static String     transliteration(String receive) {
        Transliterator transliterator;
        if (isCyrillic(receive))
            transliterator = Transliterator.getInstance("Russian-Latin/BGN");
        else
            transliterator = Transliterator.getInstance("Latin-Russian/BGN");
        return transliterator.transliterate(receive);
    }

    private static boolean isCyrillic(String text) {
        return text.chars()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }
}