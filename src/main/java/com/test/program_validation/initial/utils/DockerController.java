package com.test.program_validation.initial.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DockerController {

    public static boolean buildDockerImage(String imageName, String dockerfilePath) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "build", "-t", imageName, dockerfilePath
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[DOCKER] " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println(":white_check_mark: Docker image built successfully: " + imageName);
                return true;
            } else {
                System.err.println(":x: Docker build failed with exit code: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println(":x: Error during Docker build: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void runContainer(String imageName, Map<String, String> dockerRunCommands) {
        try {
            // Create a ProcessBuilder with basic docker run command
            List<String> commands = new ArrayList<>();
            commands.add("docker");
            commands.add("run");
            commands.add("--rm");

            // Add environment variables using -e flag
            for (Map.Entry<String, String> entry : dockerRunCommands.entrySet()) {
                commands.add("-e");
                commands.add(entry.getKey() + "=" + entry.getValue());

            }

            // Add the image name last
            commands.add(imageName);

            ProcessBuilder builder = new ProcessBuilder(commands);

            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[RUN] " + line);
                }
            }
            int exitCode = process.waitFor();
            System.out.println(":rocket: Container exited with code: " + exitCode);
        } catch (Exception e) {
            System.err.println(":x: Error running container: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
