package edu.cmu.gradiatorx.dynamic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * Configuration class for Dynamic Analysis Service
 * Centralizes path management and service configuration
 */
@Configuration
public class ServiceConfig {

    @Value("${dynamic.analysis.docker.path:docker}")
    private String dockerPath;

    @Value("${dynamic.analysis.unzip.path:docker/unzip_files}")
    private String unzipPath;

    @Value("${dynamic.analysis.submissions.path:submissions}")
    private String submissionsPath;

    @Value("${dynamic.analysis.image.name:dynamic_test}")
    private String defaultImageName;

    @Value("${dynamic.analysis.reports.path:build/reports}")
    private String reportsPath;

    /**
     * Get the absolute path to the Docker configuration directory
     *
     * @return Full path to docker directory
     */
    public String getDockerPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, dockerPath).toString();
    }

    /**
     * Get the absolute path to the unzip directory
     *
     * @return Full path to unzip directory
     */
    public String getUnzipPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, unzipPath).toString();
    }

    /**
     * Get the path for storing submission zip files
     *
     * @return Relative path to submissions directory
     */
    public String getSubmissionsPath() {
        return submissionsPath;
    }

    /**
     * Get the default Docker image name
     *
     * @return Default image name for dynamic analysis
     */
    public String getDefaultImageName() {
        return defaultImageName;
    }

    /**
     * Get the path to test reports relative to the unzip directory
     *
     * @return Relative path to test reports
     */
    public String getReportsPath() {
        return reportsPath;
    }

    /**
     * Get the full path to test reports for a submission
     *
     * @return Full path to test reports directory
     */
    public String getFullReportsPath() {
        return Paths.get(getUnzipPath(), reportsPath).toString();
    }
} 