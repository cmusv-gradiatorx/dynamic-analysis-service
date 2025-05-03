package com.test.program_validation.initial.controller;

import com.test.program_validation.initial.models.PubSubPayload;
import com.test.program_validation.initial.utils.DockerController;
import com.test.program_validation.initial.utils.UnzipSubmission;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.Base64;

@RestController
@RequestMapping("/submissions")
public class SubmissionsController {

    @PostMapping
    public void receivePush(@RequestBody PubSubPayload payload) {
        try {
            byte[] zipBytes = Base64.getDecoder().decode(payload.message.data);
            UnzipSubmission.saveZipToDisk(zipBytes);

            System.out.println(":white_check_mark: Received and saved ZIP from message: " + payload.message.messageId);

            String courseCode = payload.message.courseCode;

            // Get absolute path to the utils directory
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path utilsPath = projectRoot.resolve("src/main/java/com/test/program_validation/initial/utils");

            // Decide which Docker image to build based on the courseCode
            String imageName;
            String dockerfileName = switch (courseCode) {
                case "18656" -> {
                    imageName = "dotnet_image";
                    yield "dotnet.Docker";
                }
                case "18664" -> {
                    imageName = "java_image";
                    yield "java.Docker";
                }
                default -> throw new IllegalArgumentException("Unsupported course code: " + courseCode);
            };

            Path dockerfilePath = utilsPath.resolve(dockerfileName);

            // Build Docker image using the correct Dockerfile
            boolean isBuilt = DockerController.buildDockerImage(
                    imageName,
                    dockerfilePath.toString()
            );

            if (isBuilt) {
                DockerController.runContainer(imageName);
            }
            else {
                throw new RuntimeException("Unable to build the docker container");
            }
        } catch (Exception e) {
            System.err.println(":x: Failed to process message: " + e.getMessage());
            throw new RuntimeException("Bad Request: " + e.getMessage());
        }
    }
}
