import convert.Converter;
import convert.Point;
import org.junit.Test;

public class ConverterTest {

    Converter converter = new Converter();
    @Test
    public void parseLineTest(){
        Point p = converter.parseLine("rp99 ;865851.87;1303031.388;5.9");
        System.out.println(p);
    }
}
