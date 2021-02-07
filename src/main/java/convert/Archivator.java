package convert;

import ch.qos.logback.core.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Archivator {
    File zip;

    File output;

    String clientDirectory;

    ArrayList<File> fromArchive;

    public Archivator(File zip, String clientDirectory) {
        System.out.println(zip.getAbsolutePath());
        this.zip = zip;
        output = null;
        this.clientDirectory = clientDirectory;
        fromArchive = new ArrayList<>();
    }

    public int extractFile()
    {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            String name;
            long size;
            int j = 0;
            while ((entry = zis.getNextEntry()) != null)
            {
                name = entry.getName();
                size = entry.getSize();
                System.out.printf("name: %s, size: %d\n", name, size);

                File f = new File(clientDirectory + name + j);
                fromArchive.add(f);
                FileOutputStream fos = new FileOutputStream(new File(clientDirectory + name + j));
                int c;
                while ((c = zis.read()) != -1)
                    fos.write(c);
                fos.flush();
                fos.close();
                zis.closeEntry();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return (0);
        }
        return (1);
    }

    public ArrayList<File> getFromArchive() {
        return fromArchive;
    }
}
