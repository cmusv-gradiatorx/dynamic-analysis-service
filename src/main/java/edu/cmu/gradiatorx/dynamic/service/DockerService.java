package edu.cmu.gradiatorx.dynamic.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Docker operations using Java Docker client
 * Replaces shell command execution with proper Java API calls
 */
@Service
public class DockerService {

    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
    private final DockerClient dockerClient;

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
     * Build a Docker image from the specified directory
     *
     * @param imageName      The name to assign to the built image
     * @param dockerfilePath The directory containing the Dockerfile
     * @return true if the image was built successfully, false otherwise
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
     * Run a Docker container with specified environment variables
     *
     * @param imageName            The Docker image to run
     * @param environmentVariables Map of environment variables to pass to the container
     * @return The container execution result
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
     * Container execution result
     */
    public static class ContainerExecutionResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public ContainerExecutionResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
} 