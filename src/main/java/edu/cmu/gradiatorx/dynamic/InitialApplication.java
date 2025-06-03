package edu.cmu.gradiatorx.dynamic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Dynamic Analysis Service.
 *
 * <p>This Spring Boot application provides REST APIs for processing code submissions
 * through dynamic analysis. It handles receiving submissions, running them in isolated
 * Docker containers, and publishing results to Google Cloud Pub/Sub topics.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Concurrent submission processing with isolated containers</li>
 *   <li>Native Java Docker client integration</li>
 *   <li>Google Cloud Pub/Sub results publishing</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 *
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
public class InitialApplication {

    /**
     * Main entry point for the Dynamic Analysis Service application.
     *
     * <p>This method bootstraps the Spring Boot application context and starts
     * the embedded web server to handle HTTP requests for submission processing.</p>
     *
     * @param args command-line arguments passed to the application.
     *             Common Spring Boot arguments include:
     *             <ul>
     *               <li>--server.port=8080 (set server port)</li>
     *               <li>--spring.profiles.active=dev (set active profile)</li>
     *               <li>--debug (enable debug logging)</li>
     *             </ul>
     */
    public static void main(String[] args) {
        SpringApplication.run(InitialApplication.class, args);
    }
}
