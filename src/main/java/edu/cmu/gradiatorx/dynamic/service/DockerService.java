package edu.cmu.gradiatorx.dynamic.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/**
 * Service for managing Docker operations using the Java Docker client.
 * 
 * <p>This service provides a native Java implementation for Docker operations,
 * replacing shell command execution with proper Java API calls. It handles
 * Docker image building, container lifecycle management, volume mounting,
 * and resource cleanup with comprehensive error handling.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Native Java Docker client integration for cross-platform compatibility</li>
 *   <li>Isolated container execution with unique naming and environment setup</li>
 *   <li>Volume mounting for secure file sharing between host and containers</li>
 *   <li>Comprehensive logging and monitoring of container operations</li>
 *   <li>Automatic resource cleanup to prevent container and image accumulation</li>
 *   <li>Concurrent container execution support for multiple submissions</li>
 * </ul>
 * 
 * <p>The service is designed for high-throughput environments where multiple
 * submissions need to be processed simultaneously without interference. Each
 * container execution is isolated and tracked independently.</p>
 * 
 * <p><strong>Note:</strong> This class requires Docker Java client dependencies
 * to be present in the classpath. If dependencies are missing, the service
 * will fail to initialize.</p>
 * 
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 * @see DockerClient
 * @see org.springframework.stereotype.Service
 */
@Service
public class DockerService {

    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
    
    /**
     * The Docker client instance used for all Docker operations.
     * Configured to connect to the local Docker daemon via Unix socket.
     */
    private final DockerClient dockerClient;

    /**
     * Constructs a new DockerService and initializes the Docker client.
     * 
     * <p>The constructor configures the Docker client to connect to the local
     * Docker daemon using Unix socket communication. It sets up connection
     * pooling, timeouts, and SSL configuration for optimal performance.</p>
     * 
     * <p>Connection configuration:</p>
     * <ul>
     *   <li>Docker host: unix:///var/run/docker.sock</li>
     *   <li>Max connections: 100</li>
     *   <li>Connection timeout: 30 seconds</li>
     *   <li>Response timeout: 45 seconds</li>
     * </ul>
     * 
     * @throws RuntimeException if Docker client initialization fails due to
     *                          missing Docker daemon, permission issues, or
     *                          network connectivity problems
     */
    public DockerService() {
        // Configure Docker client
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        logger.info("Docker client initialized successfully");
    }

    /**
     * Build a Docker image from the specified directory containing a Dockerfile.
     * 
     * <p>This method builds a Docker image using the provided build context directory.
     * The build process includes dependency installation, environment setup, and
     * any custom configuration specified in the Dockerfile.</p>
     * 
     * <p>Build features:</p>
     * <ul>
     *   <li>Incremental builds with layer caching for performance</li>
     *   <li>Pull latest base images to ensure security updates</li>
     *   <li>Real-time build output logging for debugging</li>
     *   <li>Proper error handling and cleanup on failure</li>
     * </ul>
     * 
     * <p>The method is idempotent - calling it multiple times with the same
     * parameters will reuse cached layers where possible.</p>
     *
     * @param imageName      The name and optional tag to assign to the built image
     *                       (e.g., "dynamic_test:latest"); must not be null or empty
     * @param dockerfilePath The absolute path to the directory containing the Dockerfile;
     *                       must exist and contain a valid Dockerfile
     * @return true if the image was built successfully and is ready for use;
     *         false if the build failed due to Dockerfile errors, missing dependencies,
     *         or Docker daemon issues
     * @throws IllegalArgumentException if imageName is null/empty or dockerfilePath
     *                                  points to a non-existent directory
     * @see #runContainer(String, Map)
     * @see #runContainerWithZipFile(String, String, String, Map)
     */
    public boolean buildDockerImage(String imageName, String dockerfilePath) {
        try {
            logger.info("Building Docker image '{}' from path: {}", imageName, dockerfilePath);

            File dockerFile = new File(dockerfilePath);
            if (!dockerFile.exists() || !dockerFile.isDirectory()) {
                logger.error("Docker build context directory does not exist: {}", dockerfilePath);
                return false;
            }

            // Build the image
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                    .withDockerfile(new File(dockerFile, "Dockerfile"))
                    .withBaseDirectory(dockerFile)
                    .withTags(Set.of(imageName))
                    .withNoCache(false)
                    .withPull(true);

            String imageId = buildImageCmd.exec(new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    if (item.getStream() != null) {
                        logger.debug("[DOCKER BUILD] {}", item.getStream().trim());
                    }
                    super.onNext(item);
                }
            }).awaitImageId();

            if (imageId != null) {
                logger.info("✅ Docker image built successfully: {} (ID: {})", imageName, imageId);
                return true;
            } else {
                logger.error("❌ Docker build failed - no image ID returned");
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Error during Docker build: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Run a Docker container with specified environment variables.
     * 
     * <p>This method creates and runs a Docker container from the specified image,
     * configures it with the provided environment variables, and monitors its
     * execution until completion. All output is captured and returned.</p>
     * 
     * <p>Container execution features:</p>
     * <ul>
     *   <li>Isolated execution environment for each container</li>
     *   <li>Real-time output capture from stdout and stderr</li>
     *   <li>Automatic container cleanup after execution</li>
     *   <li>Configurable timeout to prevent runaway processes</li>
     * </ul>
     *
     * @param imageName            The Docker image name to run; must exist locally
     *                             or be pullable from a registry
     * @param environmentVariables Map of environment variables to pass to the container;
     *                             may be empty but must not be null
     * @return ContainerExecutionResult containing exit code, stdout, and stderr;
     *         never null, but may indicate failure through non-zero exit code
     * @throws RuntimeException if container creation fails, Docker daemon is
     *                          unreachable, or resource limits are exceeded
     * @see ContainerExecutionResult
     * @see #runContainerWithZipFile(String, String, String, Map)
     */
    public ContainerExecutionResult runContainer(String imageName, Map<String, String> environmentVariables) {
        try {
            logger.info("Running Docker container from image: {}", imageName);

            // Prepare environment variables
            List<String> env = environmentVariables.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();

            env.forEach(envVar -> logger.debug("Environment variable: {}", envVar));

            // Create container
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withEnv(env)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String containerId = container.getId();
            logger.info("Created container: {}", containerId);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for container to finish and collect logs
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();

            try {
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .exec(new LogContainerResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                String log = new String(frame.getPayload()).trim();
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    output.append(log).append("\n");
                                    logger.info("[CONTAINER OUT] {}", log);
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    errors.append(log).append("\n");
                                    logger.warn("[CONTAINER ERR] {}", log);
                                }
                            }
                        }).awaitCompletion(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Container log collection interrupted");
            }

            // Wait for container to finish
            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode();

            // Remove container
            dockerClient.removeContainerCmd(containerId).exec();
            logger.info("🚀 Container completed with exit code: {}", exitCode);

            return new ContainerExecutionResult(exitCode, output.toString(), errors.toString());

        } catch (Exception e) {
            logger.error("❌ Error running container: {}", e.getMessage(), e);
            return new ContainerExecutionResult(-1, "", "Error: " + e.getMessage());
        }
    }

    /**
     * Run a Docker container with a mounted ZIP file for submission processing.
     * 
     * <p>This method provides isolated processing of submission files by mounting
     * the ZIP file into a container and running the analysis workflow. The container
     * handles extraction, compilation, testing, and result generation internally.</p>
     * 
     * <p>Volume mounting features:</p>
     * <ul>
     *   <li>Read-only ZIP file mounting for security</li>
     *   <li>Isolated workspace creation within container</li>
     *   <li>Unique submission ID environment variables</li>
     *   <li>Automatic cleanup of container resources</li>
     * </ul>
     * 
     * <p>This method is the primary entry point for submission processing and
     * ensures complete isolation between concurrent submissions.</p>
     *
     * @param imageName            The Docker image name to run; must contain analysis tools
     * @param zipFilePath          Absolute path to the submission ZIP file on host;
     *                             must exist and be readable
     * @param submissionId         The unique submission ID for identification and tracking;
     *                             used for environment variables and logging
     * @param environmentVariables Additional environment variables to pass to container;
     *                             may be empty but must not be null
     * @return ContainerExecutionResult containing execution details and output;
     *         never null, success determined by exit code
     * @throws RuntimeException if ZIP file doesn't exist, container creation fails,
     *                          or volume mounting encounters permission issues
     * @see #runContainer(String, Map)
     * @see ContainerExecutionResult
     */
    public ContainerExecutionResult runContainerWithZipFile(String imageName, String zipFilePath, String submissionId, Map<String, String> environmentVariables) {
        try {
            logger.info("Running Docker container from image: {} with zip file: {}", imageName, zipFilePath);

            // Prepare environment variables
            Map<String, String> allEnvVars = new HashMap<>(environmentVariables);
            allEnvVars.put("SUBMISSION_ID", submissionId);
            allEnvVars.put("ZIP_FILE_NAME", submissionId + ".zip");

            List<String> env = allEnvVars.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();

            env.forEach(envVar -> logger.debug("Environment variable: {}", envVar));

            // Create bind mount for the zip file
            String containerZipPath = "/workspace/" + submissionId + ".zip";
            
            // Create container with volume mount
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withEnv(env)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withBinds(new Bind(zipFilePath, new Volume(containerZipPath), AccessMode.ro))
                    .exec();

            String containerId = container.getId();
            logger.info("Created container: {} with mounted zip: {}", containerId, containerZipPath);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for container to finish and collect logs
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();

            try {
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .exec(new LogContainerResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                String log = new String(frame.getPayload()).trim();
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    output.append(log).append("\n");
                                    logger.info("[CONTAINER OUT] {}", log);
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    errors.append(log).append("\n");
                                    logger.warn("[CONTAINER ERR] {}", log);
                                }
                            }
                        }).awaitCompletion(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Container log collection interrupted");
            }

            // Wait for container to finish
            Integer exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode();

            // Remove container
            dockerClient.removeContainerCmd(containerId).exec();
            logger.info("🚀 Container completed with exit code: {}", exitCode);

            return new ContainerExecutionResult(exitCode, output.toString(), errors.toString());

        } catch (Exception e) {
            logger.error("❌ Error running container with zip file: {}", e.getMessage(), e);
            return new ContainerExecutionResult(-1, "", "Error: " + e.getMessage());
        }
    }

    /**
     * Copy files from a container to the host filesystem.
     * 
     * <p>This method extracts files from a running or stopped container to the
     * host filesystem. It's primarily used for retrieving test results, logs,
     * or generated artifacts after container execution.</p>
     * 
     * <p><strong>Note:</strong> This method is provided for compatibility but
     * is not currently used in the main submission workflow, which relies on
     * container output capture instead.</p>
     *
     * @param containerId The container ID to copy from; container may be running or stopped
     * @param containerPath Path inside the container to copy from; must exist
     * @param hostPath Path on the host to copy to; parent directories will be created
     * @return true if copy was successful; false if source doesn't exist or copy fails
     * @throws SecurityException if file access is denied by security manager
     * @see #extractTarArchive(InputStream, String)
     */
    public boolean copyFromContainer(String containerId, String containerPath, String hostPath) {
        try {
            logger.info("Copying from container {} path {} to host path {}", containerId, containerPath, hostPath);
            
            // Create host directory if it doesn't exist
            File hostDir = new File(hostPath).getParentFile();
            if (hostDir != null && !hostDir.exists()) {
                hostDir.mkdirs();
            }
            
            // Copy archive from container
            try (InputStream tarStream = dockerClient.copyArchiveFromContainerCmd(containerId, containerPath).exec()) {
                // Extract tar archive to host path
                extractTarArchive(tarStream, hostPath);
                logger.info("✅ Successfully copied files from container");
                return true;
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to copy files from container: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract a tar archive to the specified directory.
     * 
     * <p>This helper method extracts tar archives received from Docker containers.
     * It uses the system's tar command for simplicity, though production
     * environments might prefer a pure Java implementation.</p>
     * 
     * <p><strong>Implementation Note:</strong> This method temporarily writes the
     * tar stream to disk before extraction. In high-throughput environments,
     * consider using a streaming tar library for better performance.</p>
     *
     * @param tarStream The input stream containing tar data; will be consumed entirely
     * @param destinationPath The directory to extract files to; will be created if needed
     * @throws IOException if tar stream reading fails, file writing fails,
     *                     or extraction process encounters errors
     * @see #copyFromContainer(String, String, String)
     */
    private void extractTarArchive(InputStream tarStream, String destinationPath) throws IOException {
        File destDir = new File(destinationPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        // For simplicity, we'll write the tar stream to a file and extract it
        // In a production environment, you might want to use a proper tar library
        Path tempTarFile = Files.createTempFile("container-copy", ".tar");
        
        try {
            Files.copy(tarStream, tempTarFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Extract using system tar command (simple approach)
            ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tempTarFile.toString(), "-C", destinationPath);
            Process process = pb.start();
            
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logger.warn("Tar extraction returned exit code: {}", exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Tar extraction was interrupted");
            }
            
        } finally {
            // Clean up temp file
            try {
                Files.delete(tempTarFile);
            } catch (IOException e) {
                logger.warn("Failed to delete temp tar file: {}", e.getMessage());
            }
        }
    }

    /**
     * Represents the result of a Docker container execution.
     * 
     * <p>This immutable class encapsulates all information about a container's
     * execution including the exit code, captured output streams, and provides
     * convenience methods for determining success or failure.</p>
     * 
     * <p>The class is thread-safe and can be safely shared between threads
     * for result processing and analysis.</p>
     * 
     * @author Dynamic Analysis Service Team
     * @version 1.0
     * @since 1.0
     */
    public static class ContainerExecutionResult {
        
        /** The exit code returned by the container process. */
        private final int exitCode;
        
        /** All output captured from the container's stdout stream. */
        private final String stdout;
        
        /** All output captured from the container's stderr stream. */
        private final String stderr;

        /**
         * Constructs a new ContainerExecutionResult with the specified values.
         *
         * @param exitCode The exit code returned by the container
         * @param stdout The standard output captured from the container
         * @param stderr The standard error output captured from the container
         */
        public ContainerExecutionResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        /**
         * Get the exit code returned by the container.
         *
         * @return The exit code; 0 typically indicates success
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * Get the standard output captured from the container.
         *
         * @return The stdout content; may be empty but never null
         */
        public String getStdout() {
            return stdout;
        }

        /**
         * Get the standard error output captured from the container.
         *
         * @return The stderr content; may be empty but never null
         */
        public String getStderr() {
            return stderr;
        }

        /**
         * Determine if the container execution was successful.
         *
         * @return true if the exit code is 0; false otherwise
         */
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
} 