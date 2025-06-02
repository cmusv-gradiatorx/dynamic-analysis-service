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

    @Value("${dynamic.analysis.image.name:dynamic_test}")
    private String defaultImageName;

    /**
     * Get the absolute path to the Docker configuration directory
     * @return Full path to docker directory
     */
    public String getDockerPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, dockerPath).toString();
    }

    /**
     * Get the absolute path to the unzip directory
     * @return Full path to unzip directory
     */
    public String getUnzipPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, unzipPath).toString();
    }

    /**
     * Get the default Docker image name
     * @return Default image name for dynamic analysis
     */
    public String getDefaultImageName() {
        return defaultImageName;
    }
} 