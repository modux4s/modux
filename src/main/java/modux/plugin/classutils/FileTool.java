package modux.plugin.classutils;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;

public class FileTool {

    static public void unzip(File fileIn, File dir) throws IOException {
        TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(fileIn)
                        )
                )
        );
        byte[] b = new byte[4096];
        TarArchiveEntry tarEntry;

        while ((tarEntry = tarIn.getNextTarEntry()) != null) {
            final File file = new File(dir, tarEntry.getName());
            if (tarEntry.isDirectory()) {
                if (!file.mkdirs()) {
                    throw new IOException("Unable to create folder " + file.getAbsolutePath());
                }
            } else {
                final File parent = file.getParentFile();
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        throw new IOException("Unable to create folder " + parent.getAbsolutePath());
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int r;
                    while ((r = tarIn.read(b)) != -1) {
                        fos.write(b, 0, r);
                    }
                }
            }
        }

    }

}
