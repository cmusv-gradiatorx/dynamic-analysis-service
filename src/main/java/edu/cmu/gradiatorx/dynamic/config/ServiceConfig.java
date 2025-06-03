package edu.cmu.gradiatorx.dynamic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * Configuration class for Dynamic Analysis Service.
 *
 * <p>This class centralizes all configuration management for the service,
 * including path management, service settings, and environment-specific
 * configurations. It uses Spring's {@code @Value} annotation to inject
 * properties from application.properties or environment variables.</p>
 *
 * <p>The configuration supports both relative and absolute paths, automatically
 * resolving relative paths based on the current working directory. This design
 * enables flexible deployment across different environments while maintaining
 * consistent behavior.</p>
 *
 * <p>All path-related methods return absolute paths to ensure consistent
 * file system operations regardless of the application's working directory.</p>
 *
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class ServiceConfig {

    /**
     * Relative path to the Docker configuration directory.
     * Contains Dockerfile, build.gradle, and other Docker-related files.
     */
    @Value("${dynamic.analysis.docker.path:docker}")
    private String dockerPath;

    /**
     * Relative path to the directory where submissions are unzipped.
     * Used for legacy compatibility; new implementations use direct ZIP mounting.
     */
    @Value("${dynamic.analysis.unzip.path:docker/unzip_files}")
    private String unzipPath;

    /**
     * Relative path to the directory where individual submission ZIP files are stored.
     * Each submission is saved with its unique submission ID as the filename.
     */
    @Value("${dynamic.analysis.submissions.path:submissions}")
    private String submissionsPath;

    /**
     * Default Docker image name used for dynamic analysis containers.
     * This image contains the analysis environment and test execution tools.
     */
    @Value("${dynamic.analysis.image.name:dynamic}")
    private String defaultImageName;

    /**
     * Relative path to test reports within the submission directory.
     * Typically points to Gradle's build/reports directory.
     */
    @Value("${dynamic.analysis.reports.path:build/reports}")
    private String reportsPath;

    /**
     * Get the absolute path to the Docker configuration directory.
     *
     * <p>This directory contains all Docker-related files including Dockerfile,
     * Gradle build scripts, and configuration templates used by containers.</p>
     *
     * @return Full absolute path to docker directory, never null
     */
    public String getDockerPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, dockerPath).toString();
    }

    /**
     * Get the absolute path to the unzip directory.
     *
     * <p>This method is maintained for backward compatibility. In the current
     * implementation, submissions are processed using direct ZIP mounting
     * rather than extraction to this directory.</p>
     *
     * @return Full absolute path to unzip directory, never null
     */
    public String getUnzipPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, unzipPath).toString();
    }

    /**
     * Get the relative path for storing submission ZIP files.
     *
     * <p>This directory stores individual submission ZIP files, each named
     * with its unique submission ID to ensure isolation and prevent conflicts
     * between concurrent submissions.</p>
     *
     * @return Relative path to submissions directory, never null
     */
    public String getSubmissionsPath() {
        return submissionsPath;
    }

    /**
     * Get the default Docker image name for dynamic analysis.
     *
     * <p>This image name is used when building and running containers for
     * submission analysis. The image should contain all necessary tools
     * for compiling and testing submitted code.</p>
     *
     * @return Default image name for dynamic analysis, never null
     */
    public String getDefaultImageName() {
        return defaultImageName;
    }

    /**
     * Get the relative path to test reports within a submission directory.
     *
     * <p>This path is relative to the submission's working directory and
     * typically points to where Gradle generates test reports and coverage data.</p>
     *
     * @return Relative path to test reports directory, never null
     */
    public String getReportsPath() {
        return reportsPath;
    }

    /**
     * Get the full absolute path to test reports for a submission.
     *
     * <p>This method combines the unzip path with the reports path to provide
     * the complete location where test reports can be found after analysis.</p>
     *
     * <p><strong>Note:</strong> This method is primarily used for legacy
     * compatibility. Current implementations may generate reports in different
     * locations depending on the container's internal structure.</p>
     *
     * @return Full absolute path to test reports directory, never null
     */
    public String getFullReportsPath() {
        return Paths.get(getUnzipPath(), reportsPath).toString();
    }
} 