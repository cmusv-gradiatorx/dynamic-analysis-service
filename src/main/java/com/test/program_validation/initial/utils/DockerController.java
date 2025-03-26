package com.test.program_validation.initial.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    public static void runContainer(String imageName) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "run", "--rm", imageName
            );
            builder.redirectErrorStream(true);
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
