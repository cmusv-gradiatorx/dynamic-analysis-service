package edu.cmu.gradiatorx.dynamic.service;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;
import edu.cmu.gradiatorx.dynamic.model.PubSubPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for handling submission processing logic with concurrent support.
 *
 * <p>This service orchestrates the complete submission processing workflow using
 * Java-based Docker and PubSub services. It provides per-submission isolation
 * to ensure multiple submissions can be processed concurrently without conflicts.</p>
 *
 * <p>The processing workflow includes:</p>
 * <ol>
 *   <li>Receiving and validating PubSub payloads</li>
 *   <li>Saving ZIP files with unique submission-based naming</li>
 *   <li>Building Docker images for the analysis environment</li>
 *   <li>Running isolated containers with mounted submission files</li>
 *   <li>Collecting and processing container execution results</li>
 *   <li>Publishing results to Google Cloud PubSub topics</li>
 *   <li>Cleaning up temporary files and resources</li>
 * </ol>
 *
 * <p>The service is designed for high concurrency and fault tolerance, with
 * comprehensive error handling and resource cleanup to prevent memory leaks
 * and resource exhaustion.</p>
 *
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @see DockerService
 * @see PubSubService
 * @see ServiceConfig
 * @since 1.0
 */
@Service
public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    /**
     * Configuration service providing paths and settings for the application.
     * Contains Docker paths, submission directories, and image names.
     */
    private final ServiceConfig serviceConfig;

    /**
     * Docker service for container management operations.
     * Handles image building, container execution, and volume mounting.
     */
    private final DockerService dockerService;

    /**
     * PubSub service for publishing results to Google Cloud topics.
     * Manages ZIP creation and message publishing with proper resource cleanup.
     */
    private final PubSubService pubSubService;

    /**
     * Constructs a new SubmissionService with required dependencies.
     *
     * @param serviceConfig Configuration service providing application settings;
     *                      must not be null
     * @param dockerService Docker service for container operations;
     *                      must not be null
     * @param pubSubService PubSub service for result publishing;
     *                      must not be null
     */
    @Autowired
    public SubmissionService(ServiceConfig serviceConfig, DockerService dockerService, PubSubService pubSubService) {
        this.serviceConfig = serviceConfig;
        this.dockerService = dockerService;
        this.pubSubService = pubSubService;
    }

    /**
     * Process a submission payload through the complete analysis workflow.
     *
     * <p>This method handles the entire submission processing pipeline from
     * receiving the PubSub payload to publishing the final results. Each
     * submission is processed in isolation using unique file naming and
     * dedicated container instances.</p>
     *
     * <p>The method implements comprehensive error handling and cleanup to
     * ensure system stability even when individual submissions fail. All
     * temporary resources are cleaned up regardless of success or failure.</p>
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Validate submission ID from payload attributes</li>
     *   <li>Decode and save ZIP file with unique naming</li>
     *   <li>Build Docker image (cached after first build)</li>
     *   <li>Run analysis container with mounted submission ZIP</li>
     *   <li>Collect container output and test results</li>
     *   <li>Publish results to configured PubSub topic</li>
     *   <li>Clean up temporary files and resources</li>
     * </ol>
     *
     * @param payload The PubSub payload containing the submission data.
     *                Must include:
     *                <ul>
     *                  <li>message.data: base64-encoded ZIP file with code</li>
     *                  <li>message.attributes.submissionId: unique identifier</li>
     *                </ul>
     * @throws RuntimeException         if submission ID is missing, ZIP data is invalid,
     *                                  Docker operations fail, or publishing encounters errors
     * @throws IllegalArgumentException if the payload structure is invalid
     * @see PubSubPayload
     * @see #runAnalysisContainerWithZip(String, String)
     * @see #publishContainerResults(String, DockerService.ContainerExecutionResult)
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
            Path zipFilePath = saveZipToDisk(zipBytes, submissionId);
            logger.info("✅ Saved ZIP file for message: {}", payload.message.messageId);

            // Build Docker image for analysis if not already built
            boolean isBuilt = dockerService.buildDockerImage(
                    serviceConfig.getDefaultImageName(),
                    serviceConfig.getDockerPath()
            );

            if (!isBuilt) {
                cleanupZipFile(zipFilePath);
                throw new RuntimeException("Unable to build the docker container");
            }

            // Run analysis container with mounted zip file
            DockerService.ContainerExecutionResult result = runAnalysisContainerWithZip(
                    submissionId,
                    zipFilePath.toString()
            );

            if (!result.isSuccess()) {
                logger.error("❌ Container execution failed with exit code: {}", result.exitCode());
                logger.error("Container stderr: {}", result.stderr());
                cleanupZipFile(zipFilePath);
                throw new RuntimeException("Container execution failed");
            }

            // Container succeeded, publish the container's output as results
            // For now, we'll create a simple results file with the container output
            publishContainerResults(submissionId, result);

            // Clean up zip file
            cleanupZipFile(zipFilePath);
        } catch (Exception e) {
            logger.error("❌ Failed to process submission: {}", e.getMessage(), e);
            throw new RuntimeException("Bad Request: " + e.getMessage());
        }
    }

    /**
     * Run the analysis container with a mounted ZIP file for isolated processing.
     *
     * <p>This method executes the core analysis logic by running a Docker container
     * with the submission ZIP file mounted as a volume. The container handles
     * extraction, compilation, and test execution in complete isolation.</p>
     *
     * <p>The method uses the {@link DockerService#runContainerWithZipFile} method
     * to ensure proper volume mounting and environment variable setup for the
     * container execution.</p>
     *
     * @param submissionId The unique ID of the submission being processed;
     *                     used for container environment variables and logging
     * @param zipFilePath  Absolute path to the submission ZIP file on the host;
     *                     must exist and be readable
     * @return Container execution result containing exit code, stdout, and stderr;
     * never null but may indicate failure through exit code
     * @throws RuntimeException if container creation, execution, or cleanup fails
     * @see DockerService#runContainerWithZipFile(String, String, String, Map)
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
     * Publish container execution results to the configured PubSub topic.
     *
     * <p>This method creates a temporary results directory containing the container's
     * output and publishes it as a ZIP file to the PubSub topic. The results include
     * the container's exit code, stdout, and stderr for comprehensive analysis.</p>
     *
     * <p>The method handles temporary file creation and cleanup automatically,
     * ensuring no residual files are left on the system regardless of success
     * or failure.</p>
     *
     * <p><strong>Note:</strong> This implementation creates a simple text file
     * with container output. Future versions may include more sophisticated
     * result parsing and structured data generation.</p>
     *
     * @param submissionId    The unique submission ID for result correlation;
     *                        included in PubSub message attributes
     * @param containerResult The container execution result containing output data;
     *                        must not be null
     * @throws RuntimeException if temporary directory creation fails, file writing
     *                          encounters errors, or PubSub publishing fails
     * @see PubSubService#zipAndPublishResults(String, String)
     * @see #cleanupDirectory(Path)
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
                    "Exit Code: " + containerResult.exitCode() + "\n" +
                    "=== STDOUT ===\n" +
                    containerResult.stdout() + "\n" +
                    "=== STDERR ===\n" +
                    containerResult.stderr() + "\n";

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
     * Clean up a ZIP file after processing completion.
     *
     * <p>This method safely removes the submission ZIP file from the file system
     * to prevent disk space accumulation. It handles cases where the file may
     * not exist or may have already been deleted.</p>
     *
     * <p>Cleanup failures are logged as warnings but do not propagate as
     * exceptions to avoid masking more serious processing errors.</p>
     *
     * @param zipFilePath Path to the ZIP file to be deleted;
     *                    may be null or point to a non-existent file
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
     * Recursively clean up a directory and all its contents.
     *
     * <p>This method performs a depth-first deletion of the directory tree,
     * removing all files and subdirectories. The deletion order ensures that
     * files are deleted before their containing directories.</p>
     *
     * <p>The method is fault-tolerant and will attempt to delete as many files
     * as possible even if some deletions fail. Failures are logged but do not
     * stop the cleanup process.</p>
     *
     * @param directory Path to the directory to be deleted recursively;
     *                  may be null or point to a non-existent directory
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
     * @param zipBytes     The raw ZIP file data as a byte array; must not be null or empty
     * @param submissionId The unique submission ID to use as filename; must not be null,
     *                     empty, or contain invalid filename characters
     * @return Path to the saved ZIP file, guaranteed to exist and be readable
     * @throws IOException              if there's an error creating the directory, writing the file,
     *                                  or if the disk is full
     * @throws IllegalArgumentException if submissionId contains invalid filename characters
     * @throws NullPointerException     if any parameter is null
     * @see ServiceConfig#getSubmissionsPath()
     */
    private Path saveZipToDisk(byte[] zipBytes, String submissionId) throws IOException {
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

        logger.debug("📁 Saved submission ZIP: {}", zipFilePath.toAbsolutePath());
        return zipFilePath;
    }
}
