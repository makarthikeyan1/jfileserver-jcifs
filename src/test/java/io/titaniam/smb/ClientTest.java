package io.titaniam.smb;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.smb.session.SmbConfig;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbShare;

import java.io.*;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientTest {

    private SmbSession userSession;
    private String dirName;

    @BeforeEach
    private void setup() throws IOException {

        SmbConfig config = new SmbConfig();
        config.setHost("localhost");
        config.setPort(1445);
        config.setUsername("user");
        config.setPassword("java");
        config.setShareAndDir("JFILESHARE");

        SmbShare smbShare = new SmbShare(config);
        userSession =  new SmbSession(smbShare);

        dirName = "test_" + System.currentTimeMillis();
    }

    @AfterEach
    private void tearDown() {
        userSession.close();
    }

    @Test
    public void testWriteLargeFile() throws IOException {

        // 1. create dir
        createDirectory(dirName);

        // 2. create the large file on SMB
        File file500K = new File("src/test/resources/test-data/video-short.mp4");
        assertTrue(file500K.exists());

        String remoteFilePath = dirName + "\\video-short.mp4";

        createFile(remoteFilePath,
                new FileInputStream(file500K)
        );

        // 3. read contents of file and copy it to temp file
        SmbFile fromServer = userSession.createSmbFileObject(remoteFilePath);
        File tempFile = File.createTempFile("junit",".mp4");
        tempFile.deleteOnExit();

        try(SmbFileInputStream inputStream=fromServer.openInputStream();
            FileOutputStream fos = new FileOutputStream(tempFile)) {
            copy(inputStream, fos);
            fos.flush();
        }
        fromServer.close();

        // 4. assert they are exactly the same
        assertEquals(
                Files.mismatch(tempFile.toPath(), file500K.toPath()),
                -1,
                "smb file mismatched with the local copy"
        );
    }

    //=============helpers============//
    void createDirectory(String dirName) throws IOException {
        SmbFile directory = userSession.createSmbDirectoryObject(dirName);
        directory.mkdirs();
        directory.close();
    }

    void createFile(String fileName, InputStream fileContents) throws IOException {
        SmbFile remoteFile = userSession.createSmbFileObject(fileName);
        remoteFile.createNewFile();

        try(OutputStream outputStream = remoteFile.openOutputStream()) {
            copy(fileContents, outputStream);
            outputStream.flush();
        }
        remoteFile.close();
    }

    void copy(InputStream input, OutputStream output) throws IOException {

        byte[] buf = new byte[8192];
        int length;
        while ((length = input.read(buf)) != -1) {
            output.write(buf, 0, length);
        }
    }
}
