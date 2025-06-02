package edu.cmu.gradiatorx.dynamic.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing Docker operations in the Dynamic Analysis Service
 * Handles building Docker images and running containers for code analysis
 */
@Component
public class DockerController {

    private static final Logger logger = LoggerFactory.getLogger(DockerController.class);

    /**
     * Build a Docker image from the specified dockerfile path
     * @param imageName The name to assign to the built image
     * @param dockerfilePath The path containing the Dockerfile
     * @return true if the image was built successfully, false otherwise
     */
    public static boolean buildDockerImage(String imageName, String dockerfilePath) {
        try {
            logger.info("Building Docker image '{}' from path: {}", imageName, dockerfilePath);
            
            // Build the Docker image in the specified file path
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "build", "-t", imageName, dockerfilePath
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[DOCKER BUILD] {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("✅ Docker image built successfully: {}", imageName);
                return true;
            } else {
                logger.error("❌ Docker build failed with exit code: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error during Docker build: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Run a Docker container with specified environment variables
     * @param imageName The Docker image to run
     * @param dockerRunCommands Map of environment variables to pass to the container
     */
    public static void runContainer(String imageName, Map<String, String> dockerRunCommands) {
        try {
            logger.info("Running Docker container from image: {}", imageName);
            
            // Create a ProcessBuilder with basic docker run command
            List<String> commands = new ArrayList<>();
            commands.add("docker");
            commands.add("run");
            commands.add("--rm");

            // Add environment variables using -e flag
            for (Map.Entry<String, String> entry : dockerRunCommands.entrySet()) {
                commands.add("-e");
                commands.add(entry.getKey() + "=" + entry.getValue());
                logger.debug("Adding environment variable: {}={}", entry.getKey(), entry.getValue());
            }

            // Add the image name last
            commands.add(imageName);

            ProcessBuilder builder = new ProcessBuilder(commands);
            Process process = builder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[CONTAINER] {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            logger.info("🚀 Container exited with code: {}", exitCode);
            
        } catch (Exception e) {
            logger.error("❌ Error running container: {}", e.getMessage(), e);
        }
    }
}
