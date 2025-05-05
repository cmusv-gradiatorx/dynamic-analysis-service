package com.test.program_validation.initial.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipSubmission {
    public static void saveZipToDisk(byte[] zipBytes) throws IOException {

        // Specify target location for unzipped files
        String OUTPUT_DIR = "/Users/monoid/Documents/GitHub/dynamic-analysis-service/src/main/java/com/test/program_validation/initial/utils/unzip_files";
        File outputDir = new File(OUTPUT_DIR);

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
