package Helper;


//import com.sun.org.apache.bcel.internal.generic.ArrayElementValueGen;

// Структура для хранения распаршенного файла, для дальнейшего добавления в MySQL
public class SystemParam {
    public String params;
    public String sk;
    public String type;
    public String zone;

    public SystemParam(String params, String sk, String type, String zone) {
        this.params = params;
        this.type = type;
        this.zone = zone;
        this.sk = sk;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", sk,  type, zone);
    }
}
