import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ROMLoader {

    public static byte[] loadROM(String filename) throws IOException {
        return Files.readAllBytes(Path.of(filename));
    }

    public static byte[] loadBIOS(String path) throws IOException {
        File file = new File(path);
        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        int bytesRead = fis.read(data);
        fis.close();

        if (bytesRead != data.length) {
            throw new IOException("Could not read full BIOS file.");
        }

        return data;
    }


}
