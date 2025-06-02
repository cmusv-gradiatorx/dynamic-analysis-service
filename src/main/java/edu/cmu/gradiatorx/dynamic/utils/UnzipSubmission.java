package edu.cmu.gradiatorx.dynamic.utils;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipSubmission {
    public static void saveZipToDisk(byte[] zipBytes, ServiceConfig serviceConfig) throws IOException {

        // Use configured unzip path
        String outputDirPath = serviceConfig.getUnzipPath();
        File outputDir = new File(outputDirPath);

        if (!outputDir.exists()) outputDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(outputDir, entry.getName());
                outFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    zis.transferTo(fos);
                }
                System.out.println(":page_facing_up: Saved: " + outFile.getAbsolutePath());
                zis.closeEntry();
            }
        }
    }
}
