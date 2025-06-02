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
 * Service for handling Google Cloud PubSub operations
 * Replaces the Python zip_and_publish.py functionality
 */
@Service
public class PubSubService {

    private static final Logger logger = LoggerFactory.getLogger(PubSubService.class);

    @Value("${google.cloud.project.id:gradiatorx}")
    private String projectId;

    @Value("${dynamic.analysis.pubsub.topic:dynamic-analysis-result}")
    private String topicId;

    /**
     * Zip test results and publish to PubSub topic
     *
     * @param submissionId The submission ID to include in message attributes
     * @param reportsPath  Path to the test reports directory
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
     * Create a ZIP file from the specified directory
     *
     * @param directoryPath Path to directory to zip
     * @return Byte array of the ZIP file
     * @throws IOException if there's an error creating the zip
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
     * Publish a message to the PubSub topic
     *
     * @param data         The base64-encoded data to publish
     * @param submissionId The submission ID for message attributes
     * @throws Exception if publishing fails
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