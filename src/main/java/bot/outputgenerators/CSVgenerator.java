package bot.outputgenerators;

import bot.Client;
import bot.enums.TransType;
import convert.Point;
import org.osgeo.proj4j.Proj4jException;

import java.io.*;
import java.util.LinkedList;

public class CSVgenerator  extends GeneratorManager {

    public CSVgenerator(Client client) {
        super(client);
    }


}
