package bot.outputgenerators;

import bot.Client;
import bot.enums.OutputFileType;
import convert.Transformator;
import org.osgeo.proj4j.Proj4jException;

import java.io.File;
import java.io.IOException;

public class GeneratorManager {

    Client client;

    Transformator transformator;

    File output;

    public GeneratorManager(Client client) {
        this.client = client;
        if (client.getTransformationParametrs() != null)
            transformator = new Transformator(client.getTransformationParametrs());
        output = new File(client.getSavePath());
    }

    public GeneratorManager() {

    }

    public int run()
    {
        if (client.getOutputFileType() == OutputFileType.CSV)
        {
            CSVgenerator csv = new CSVgenerator();
            try {
                csv.write();
            }
            catch (IOException e) {
                return (-1);
            }
            catch (Proj4jException e) {
                return (0);
            }
        }
        return (1);
    }

    public File getOutput() {
        return output;
    }
}
