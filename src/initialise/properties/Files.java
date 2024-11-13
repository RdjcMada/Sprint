package initialise.properties;

import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class Files {
    //Attributes
    private byte[] content;
    private String fileName;

    //Constructor
    public Files(byte[] content, String fileName) {
        this.content = content;
        this.fileName = fileName;
    }

    // Getters and Setters
    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    //Function that will be used 
    public static Files convertPartToFile(Part part, String fileName) throws IOException {
        byte[] content = convertPartToBytes(part);
        return new Files(content, fileName);
    }

    private static byte[] convertPartToBytes(Part part) throws IOException {
        try (InputStream inputStream = part.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}
