package edu.cmu.gradiatorx.dynamic.controller;

import edu.cmu.gradiatorx.dynamic.models.PubSubPayload;
import edu.cmu.gradiatorx.dynamic.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for handling assignment submissions
 * Delegates processing to SubmissionService
 */
@RestController
@RequestMapping("/submissions")
@Tag(name = "Submissions", description = "Assignment Submission API")
public class SubmissionsController {

    private final SubmissionService submissionService;

    @Autowired
    public SubmissionsController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(
            summary = "Submit assignment for dynamic analysis",
            description = "Runs test suite on assignment and publishes result to PubSub topic"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission received and processing started",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "400", description = "Invalid submission data",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal processing error",
                    content = @Content)
    })
    public ResponseEntity<String> receivePush(@RequestBody PubSubPayload payload) {
        try {
            submissionService.processSubmission(payload);
            return ResponseEntity.ok("Submission received and processing started");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to process submission: " + e.getMessage());
        }
    }
}
