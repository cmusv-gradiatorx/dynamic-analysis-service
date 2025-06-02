package edu.cmu.gradiatorx.dynamic.service;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;
import edu.cmu.gradiatorx.dynamic.models.PubSubPayload;
import edu.cmu.gradiatorx.dynamic.utils.UnzipSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for handling submission processing logic
 * Uses Java-based Docker and PubSub services instead of shell commands
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
     * Process a submission payload by extracting files, building Docker image, and running analysis
     *
     * @param payload The PubSub payload containing the submission data
     * @throws RuntimeException if processing fails
     */
    public void processSubmission(PubSubPayload payload) {
        try {
            // Extract and save submission files
            byte[] zipBytes = Base64.getDecoder().decode(payload.message.data);
            UnzipSubmission.saveZipToDisk(zipBytes, serviceConfig);
            logger.info("✅ Received and saved ZIP from message: {}", payload.message.messageId);

            String submissionId = payload.message.attributes.get("submissionId");
            logger.info("Processing submission ID: {}", submissionId);

            // Build Docker image for analysis
            boolean isBuilt = dockerService.buildDockerImage(
                    serviceConfig.getDefaultImageName(),
                    serviceConfig.getDockerPath()
            );

            if (isBuilt) {
                // Run analysis container and get results
                DockerService.ContainerExecutionResult result = runAnalysisContainer(submissionId);

                if (result.isSuccess()) {
                    // Container succeeded, now zip and publish results from the host
                    publishAnalysisResults(submissionId);
                } else {
                    logger.error("❌ Container execution failed with exit code: {}", result.getExitCode());
                    logger.error("Container stderr: {}", result.getStderr());
                    throw new RuntimeException("Container execution failed");
                }
            } else {
                throw new RuntimeException("Unable to build the docker container");
            }

        } catch (Exception e) {
            logger.error("❌ Failed to process submission: {}", e.getMessage(), e);
            throw new RuntimeException("Bad Request: " + e.getMessage());
        }
    }

    /**
     * Run the analysis container with the specified submission ID
     *
     * @param submissionId The ID of the submission being processed
     * @return Container execution result
     */
    private DockerService.ContainerExecutionResult runAnalysisContainer(String submissionId) {
        Map<String, String> dockerRunCommands = new HashMap<>();
        dockerRunCommands.put("SUBMISSION_ID", submissionId);

        logger.info("Starting analysis container for submission: {}", submissionId);
        return dockerService.runContainer(serviceConfig.getDefaultImageName(), dockerRunCommands);
    }

    /**
     * Zip and publish analysis results to PubSub topic
     *
     * @param submissionId The submission ID
     */
    private void publishAnalysisResults(String submissionId) {
        try {
            // Get the full path to test reports
            String reportsPath = serviceConfig.getFullReportsPath();

            logger.info("Publishing results from: {}", reportsPath);
            pubSubService.zipAndPublishResults(submissionId, reportsPath);

        } catch (Exception e) {
            logger.error("❌ Failed to publish results for submission {}: {}", submissionId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish analysis results", e);
        }
    }
} 