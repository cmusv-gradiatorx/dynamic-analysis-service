package edu.cmu.gradiatorx.dynamic.service;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;
import edu.cmu.gradiatorx.dynamic.models.PubSubPayload;
import edu.cmu.gradiatorx.dynamic.utils.UnzipSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for handling submission processing logic
 * Uses Java-based Docker and PubSub services with per-submission isolation
 */
@Service
public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    private final ServiceConfig serviceConfig;
    private final DockerService dockerService;
    private final PubSubService pubSubService;

    @Autowired
    public SubmissionService(ServiceConfig serviceConfig, DockerService dockerService, PubSubService pubSubService) {
        this.serviceConfig = serviceConfig;
        this.dockerService = dockerService;
        this.pubSubService = pubSubService;
    }

    /**
     * Process a submission payload by saving zip file and running Docker analysis
     *
     * @param payload The PubSub payload containing the submission data
     * @throws RuntimeException if processing fails
     */
    public void processSubmission(PubSubPayload payload) {
        String submissionId = payload.message.attributes.get("submissionId");
        
        if (submissionId == null || submissionId.trim().isEmpty()) {
            throw new RuntimeException("Submission ID is required");
        }
        
        try {
            logger.info("Processing submission ID: {}", submissionId);

            // Save zip file with submission ID as filename
            byte[] zipBytes = Base64.getDecoder().decode(payload.message.data);
            Path zipFilePath = UnzipSubmission.saveZipToDisk(zipBytes, submissionId, serviceConfig);
            logger.info("✅ Saved ZIP file for message: {}", payload.message.messageId);

            // Build Docker image for analysis if not already built
            boolean isBuilt = dockerService.buildDockerImage(
                    serviceConfig.getDefaultImageName(), 
                    serviceConfig.getDockerPath()
            );

            if (isBuilt) {
                // Run analysis container with mounted zip file
                DockerService.ContainerExecutionResult result = runAnalysisContainerWithZip(
                    submissionId, 
                    zipFilePath.toString()
                );
                
                if (result.isSuccess()) {
                    // Container succeeded, publish the container's output as results
                    // For now, we'll create a simple results file with the container output
                    publishContainerResults(submissionId, result);
                    
                    // Clean up zip file
                    cleanupZipFile(zipFilePath);
                } else {
                    logger.error("❌ Container execution failed with exit code: {}", result.getExitCode());
                    logger.error("Container stderr: {}", result.getStderr());
                    cleanupZipFile(zipFilePath);
                    throw new RuntimeException("Container execution failed");
                }
            } else {
                cleanupZipFile(zipFilePath);
                throw new RuntimeException("Unable to build the docker container");
            }

        } catch (Exception e) {
            logger.error("❌ Failed to process submission: {}", e.getMessage(), e);
            throw new RuntimeException("Bad Request: " + e.getMessage());
        }
    }

    /**
     * Run the analysis container with a mounted zip file
     *
     * @param submissionId The ID of the submission being processed
     * @param zipFilePath Path to the submission zip file
     * @return Container execution result
     */
    private DockerService.ContainerExecutionResult runAnalysisContainerWithZip(String submissionId, String zipFilePath) {
        Map<String, String> environmentVariables = new HashMap<>();
        // Additional environment variables can be added here if needed
        
        logger.info("Starting analysis container for submission: {} with zip: {}", submissionId, zipFilePath);
        return dockerService.runContainerWithZipFile(
            serviceConfig.getDefaultImageName(), 
            zipFilePath, 
            submissionId, 
            environmentVariables
        );
    }

    /**
     * Publish container results to PubSub topic
     * For now, we create a simple result file with container output
     *
     * @param submissionId The submission ID
     * @param containerResult The container execution result
     */
    private void publishContainerResults(String submissionId, DockerService.ContainerExecutionResult containerResult) {
        try {
            // Create a temporary results directory
            String currentDir = System.getProperty("user.dir");
            Path tempResultsDir = Paths.get(currentDir, "temp-results", submissionId);
            Files.createDirectories(tempResultsDir);
            
            // Create a simple results file with container output
            Path resultFile = tempResultsDir.resolve("container-output.txt");
            String resultContent = "=== CONTAINER EXECUTION RESULTS ===\n" +
                    "Exit Code: " + containerResult.getExitCode() + "\n" +
                    "=== STDOUT ===\n" +
                    containerResult.getStdout() + "\n" +
                    "=== STDERR ===\n" +
                    containerResult.getStderr() + "\n";
            
            Files.write(resultFile, resultContent.getBytes());
            
            logger.info("Publishing results from: {}", tempResultsDir);
            pubSubService.zipAndPublishResults(submissionId, tempResultsDir.toString());
            
            // Clean up temporary results
            cleanupDirectory(tempResultsDir);
            
        } catch (Exception e) {
            logger.error("❌ Failed to publish results for submission {}: {}", submissionId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish analysis results", e);
        }
    }

    /**
     * Clean up the zip file after processing
     */
    private void cleanupZipFile(Path zipFilePath) {
        try {
            if (zipFilePath != null && Files.exists(zipFilePath)) {
                Files.delete(zipFilePath);
                logger.debug("🗑️ Cleaned up zip file: {}", zipFilePath);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to clean up zip file: {}", e.getMessage());
        }
    }

    /**
     * Clean up a directory and its contents
     */
    private void cleanupDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                     .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (Exception e) {
                             logger.warn("Failed to delete: {}", path);
                         }
                     });
                logger.debug("🗑️ Cleaned up directory: {}", directory);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to clean up directory {}: {}", directory, e.getMessage());
        }
    }
} 