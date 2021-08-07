import convert.InfoReader;
import convert.Point;
import org.junit.Test;

public class ConverterTest {

    InfoReader converter = new InfoReader();
    @Test
    public void parseLineTest() throws Exception {
        Point p = converter.parseLine("rp99 ;865851.87;1303031.388;5.9");
        System.out.println(p);
    }
}
