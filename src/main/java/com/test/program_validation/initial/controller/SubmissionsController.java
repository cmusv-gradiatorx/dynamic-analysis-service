package com.test.program_validation.initial.controller;

import com.test.program_validation.initial.models.PubSubPayload;
import com.test.program_validation.initial.utils.DockerController;
import com.test.program_validation.initial.utils.UnzipSubmission;
import org.springframework.web.bind.annotation.*;

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
            boolean isBuilt = DockerController.buildDockerImage("dynamic_test", "/Users/monoid/Documents/GitHub/dynamic-analysis-service/src/main/java/com/test/program_validation/initial/utils");
            if (isBuilt) {
                DockerController.runContainer("dynamic_test");
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
