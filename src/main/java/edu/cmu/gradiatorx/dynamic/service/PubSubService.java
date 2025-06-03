package edu.cmu.gradiatorx.dynamic.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Service for handling Google Cloud Pub/Sub operations and result publishing.
 *
 * <p>This service replaces the Python-based {@code zip_and_publish.py} functionality
 * with native Java implementation using the Google Cloud Pub/Sub client library.
 * It handles the complete workflow of zipping test results and publishing them
 * to configured Pub/Sub topics.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Native ZIP file creation using Apache Commons Compress</li>
 *   <li>Base64 encoding for binary data transmission</li>
 *   <li>Asynchronous message publishing with proper resource management</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Automatic cleanup of temporary resources</li>
 * </ul>
 *
 * <p>The service is thread-safe and can handle concurrent publishing operations
 * from multiple submission processing threads.</p>
 *
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class PubSubService {

    private static final Logger logger = LoggerFactory.getLogger(PubSubService.class);

    /**
     * Google Cloud project ID where the Pub/Sub topic is located.
     * Injected from application properties or environment variables.
     */
    @Value("${google.cloud.project.id:gradiatorx}")
    private String projectId;

    /**
     * Name of the Pub/Sub topic where results will be published.
     * Injected from application properties with a sensible default.
     */
    @Value("${dynamic.analysis.pubsub.topic:dynamic-analysis-result}")
    private String topicId;

    /**
     * Zip test results from a directory and publish to the configured Pub/Sub topic.
     *
     * <p>This method performs the complete workflow of result publishing:</p>
     * <ol>
     *   <li>Validates the reports directory exists and contains files</li>
     *   <li>Creates a ZIP archive containing all files from the directory</li>
     *   <li>Encodes the ZIP data as base64 for transport</li>
     *   <li>Publishes the encoded data to the Pub/Sub topic with submission metadata</li>
     * </ol>
     *
     * <p>The method handles empty directories gracefully by logging a warning
     * and skipping the publishing step.</p>
     *
     * @param submissionId The unique submission ID to include in message attributes;
     *                     used for tracking and correlation in downstream systems
     * @param reportsPath  Absolute or relative path to the test reports directory;
     *                     must contain at least one file to be published
     * @throws RuntimeException if zipping fails, directory access is denied,
     *                          or Pub/Sub publishing encounters an error
     * @see #createZipFromDirectory(String)
     * @see #publishMessage(String, String)
     */
    public void zipAndPublishResults(String submissionId, String reportsPath) {
        try {
            logger.info("Zipping and publishing results for submission: {}", submissionId);

            // Create zip file from reports directory
            byte[] zipData = createZipFromDirectory(reportsPath);
            if (zipData.length == 0) {
                logger.warn("No data to zip from path: {}", reportsPath);
                return;
            }

            // Base64 encode the zip data
            String base64Zip = Base64.getEncoder().encodeToString(zipData);

            // Publish to PubSub
            publishMessage(base64Zip, submissionId);

            logger.info("✅ Successfully published test results for submission: {}", submissionId);

        } catch (Exception e) {
            logger.error("❌ Failed to zip and publish results for submission {}: {}", submissionId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish results", e);
        }
    }

    /**
     * Create a ZIP archive from all files in the specified directory.
     *
     * <p>This method recursively walks through the directory structure and includes
     * all regular files in the ZIP archive. Directory structure is preserved,
     * and file paths in the ZIP are relative to the root directory.</p>
     *
     * <p>The method uses Apache Commons Compress for ZIP creation, ensuring
     * proper handling of file metadata and cross-platform compatibility.</p>
     *
     * <p>Empty directories are ignored - only files are included in the archive.</p>
     *
     * @param directoryPath Absolute or relative path to the directory to zip;
     *                      if relative, resolved against current working directory
     * @return Byte array containing the complete ZIP file data;
     * empty array if directory doesn't exist or contains no files
     * @throws IOException       if there's an error reading files, creating the ZIP,
     *                           or if the directory is not accessible
     * @throws SecurityException if file access is denied by security manager
     */
    private byte[] createZipFromDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            logger.warn("Directory does not exist: {}", directoryPath);
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(baos)) {
            // Walk through all files in the directory
            try (Stream<Path> files = Files.walk(dirPath)) {
                files.filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                // Create relative path for the zip entry
                                Path relativePath = dirPath.relativize(file);
                                String entryName = relativePath.toString().replace('\\', '/');

                                ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
                                entry.setSize(Files.size(file));
                                zipOut.putArchiveEntry(entry);

                                Files.copy(file, zipOut);
                                zipOut.closeArchiveEntry();

                                logger.debug("Added to zip: {}", entryName);
                            } catch (IOException e) {
                                logger.error("Error adding file to zip: {}", file, e);
                            }
                        });
            }
        }

        byte[] zipData = baos.toByteArray();
        logger.info("✅ Created zip file with {} bytes from directory: {}", zipData.length, directoryPath);
        return zipData;
    }

    /**
     * Publish a message to the configured Pub/Sub topic with submission metadata.
     *
     * <p>This method creates a Pub/Sub publisher, sends the message with appropriate
     * attributes, and ensures proper resource cleanup. The publishing operation
     * is asynchronous but this method blocks until completion or failure.</p>
     *
     * <p>Message attributes include the submission ID for correlation and tracking
     * in downstream processing systems.</p>
     *
     * <p>The publisher is created fresh for each message to ensure isolation
     * and proper resource management, though this could be optimized for
     * high-throughput scenarios by connection pooling.</p>
     *
     * @param data         The base64-encoded ZIP data to publish; must not be null
     * @param submissionId The submission ID to include in message attributes;
     *                     used for message correlation and routing
     * @throws Exception            if publisher creation fails, message publishing fails,
     *                              or if authentication/authorization is denied
     * @throws InterruptedException if the publishing operation is interrupted
     * @throws ExecutionException   if the publishing operation fails on the server side
     * @see Publisher
     * @see PubsubMessage
     */
    private void publishMessage(String data, String submissionId) throws Exception {
        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {
            // Create publisher
            publisher = Publisher.newBuilder(topicName).build();

            // Create message with attributes
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(data))
                    .putAttributes("submissionId", submissionId)
                    .build();

            // Publish message
            ApiFuture<String> messageIdFuture = publisher.publish(message);
            String messageId = messageIdFuture.get();

            logger.info("📤 Published message to topic {} with ID: {}", topicId, messageId);

        } catch (ExecutionException | InterruptedException e) {
            logger.error("❌ Failed to publish message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish message", e);
        } finally {
            if (publisher != null) {
                try {
                    publisher.shutdown();
                    publisher.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Publisher shutdown interrupted");
                }
            }
        }
    }
} 