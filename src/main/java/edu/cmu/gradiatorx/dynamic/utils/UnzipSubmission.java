package edu.cmu.gradiatorx.dynamic.utils;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UnzipSubmission {
    
    /**
     * Save the zip file to disk with submission ID as filename
     * The zip file will be extracted inside the Docker container to avoid concurrency issues
     * 
     * @param zipBytes The zip file data
     * @param submissionId The unique submission ID to use as filename
     * @param serviceConfig Service configuration for paths
     * @return Path to the saved zip file
     * @throws IOException if there's an error saving the file
     */
    public static Path saveZipToDisk(byte[] zipBytes, String submissionId, ServiceConfig serviceConfig) throws IOException {
        // Create submissions directory path
        String currentDir = System.getProperty("user.dir");
        Path submissionsDir = Paths.get(currentDir, serviceConfig.getSubmissionsPath());
        
        // Ensure the submissions directory exists
        if (!Files.exists(submissionsDir)) {
            Files.createDirectories(submissionsDir);
        }
        
        // Create zip file path with submission ID as filename
        Path zipFilePath = submissionsDir.resolve(submissionId + ".zip");
        
        // Write zip file to disk
        Files.write(zipFilePath, zipBytes);
        
        System.out.println("📁 Saved submission ZIP: " + zipFilePath.toAbsolutePath());
        return zipFilePath;
    }
}
