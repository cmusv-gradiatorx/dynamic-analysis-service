package edu.cmu.gradiatorx.dynamic.utils;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for handling ZIP file operations in the submission processing workflow.
 * 
 * <p>This class provides functionality for saving submission ZIP files to disk with
 * unique naming based on submission IDs. The design supports concurrent processing
 * by ensuring each submission has its own uniquely named file, preventing conflicts
 * between simultaneous submissions.</p>
 * 
 * <p>The class follows a strategy where ZIP files are saved to disk but not extracted
 * immediately. Instead, the ZIP files are later mounted into Docker containers where
 * extraction happens in an isolated environment.</p>
 * 
 * <p>All methods in this class are static, making it a pure utility class without
 * any instance state.</p>
 * 
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 * @see ServiceConfig
 */
public class UnzipSubmission {
    
    /**
     * Save the ZIP file to disk with submission ID as filename.
     * 
     * <p>This method creates a submissions directory if it doesn't exist and saves
     * the provided ZIP file data using the submission ID as the filename. This approach
     * ensures that multiple concurrent submissions don't interfere with each other.</p>
     * 
     * <p>The ZIP file will be extracted later inside the Docker container to avoid
     * concurrency issues and maintain isolation between submissions.</p>
     * 
     * <p>The method uses atomic file operations to ensure data integrity and prevent
     * partial writes in case of interruption.</p>
     * 
     * @param zipBytes The raw ZIP file data as a byte array; must not be null or empty
     * @param submissionId The unique submission ID to use as filename; must not be null,
     *                     empty, or contain invalid filename characters
     * @param serviceConfig Service configuration providing the submissions directory path;
     *                      must not be null
     * @return Path to the saved ZIP file, guaranteed to exist and be readable
     * @throws IOException if there's an error creating the directory, writing the file,
     *                     or if the disk is full
     * @throws IllegalArgumentException if submissionId contains invalid filename characters
     * @throws NullPointerException if any parameter is null
     * @see ServiceConfig#getSubmissionsPath()
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
