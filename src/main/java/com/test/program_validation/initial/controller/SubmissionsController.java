package com.test.program_validation.initial.controller;

import com.test.program_validation.initial.models.PubSubPayload;
import com.test.program_validation.initial.utils.DockerController;
import com.test.program_validation.initial.utils.UnzipSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/submissions")
@Tag(name = "Submissions", description = "Assignment Submission API")
public class SubmissionsController {

    @PostMapping
    @Operation(
            summary = "Submit assignment for dynamic analysis",
            description = "Runs test suite on assignment and publishes result to PubSub topic"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission received",
                    content = { @Content(mediaType = "application/json") }),
            @ApiResponse(responseCode = "404", description = "Unable to process submission",
                    content = @Content)
    })
    public void receivePush(@RequestBody PubSubPayload payload) {
        try {
            byte[] zipBytes = Base64.getDecoder().decode(payload.message.data);
            UnzipSubmission.saveZipToDisk(zipBytes);
            System.out.println(":white_check_mark: Received and saved ZIP from message: " + payload.message.messageId);

            String submissionId = payload.message.attributes.get("submissionId");

            // Build image and store result
            boolean isBuilt = DockerController.buildDockerImage(
                    "dynamic_test",
                    "/Users/monoid/Documents/GitHub/dynamic-analysis-service/src/main/java/com/test/program_validation/initial/utils");

            if (isBuilt) {
                // Store runtime arguments in dockerRunCommands
                Map<String, String> dockerRunCommands = new HashMap<>();
                dockerRunCommands.put("SUBMISSION_ID", submissionId);

                DockerController.runContainer("dynamic_test", dockerRunCommands);

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
