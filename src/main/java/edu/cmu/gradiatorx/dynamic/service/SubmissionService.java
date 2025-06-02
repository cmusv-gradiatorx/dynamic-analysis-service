package edu.cmu.gradiatorx.dynamic.service;

import edu.cmu.gradiatorx.dynamic.config.ServiceConfig;
import edu.cmu.gradiatorx.dynamic.controller.DockerController;
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
 * Separates business logic from the REST controller
 */
@Service
public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    private final ServiceConfig serviceConfig;

    @Autowired
    public SubmissionService(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    /**
     * Process a submission payload by extracting files, building Docker image, and running analysis
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
            boolean isBuilt = DockerController.buildDockerImage(
                    serviceConfig.getDefaultImageName(), 
                    serviceConfig.getDockerPath()
            );

            if (isBuilt) {
                // Run analysis container with submission ID
                runAnalysisContainer(submissionId);
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
     * @param submissionId The ID of the submission being processed
     */
    private void runAnalysisContainer(String submissionId) {
        Map<String, String> dockerRunCommands = new HashMap<>();
        dockerRunCommands.put("SUBMISSION_ID", submissionId);

        logger.info("Starting analysis container for submission: {}", submissionId);
        DockerController.runContainer(serviceConfig.getDefaultImageName(), dockerRunCommands);
    }
} 