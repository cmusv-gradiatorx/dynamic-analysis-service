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
 * REST Controller for handling assignment submissions and dynamic code analysis.
 * 
 * <p>This controller provides HTTP endpoints for receiving code submissions,
 * processing them through Docker containers, and coordinating the entire
 * dynamic analysis workflow. It delegates the actual processing logic to
 * the {@link SubmissionService}.</p>
 * 
 * <p>The controller is designed to handle concurrent submissions safely,
 * with each submission processed in isolation to prevent conflicts.</p>
 * 
 * @author Dynamic Analysis Service Team
 * @version 1.0
 * @since 1.0
 * @see SubmissionService
 */
@RestController
@RequestMapping("/submissions")
@Tag(name = "Submissions", description = "Assignment Submission API")
public class SubmissionsController {

    /**
     * Service responsible for the core submission processing logic.
     * Handles Docker container management, ZIP file processing,
     * and result publication.
     */
    private final SubmissionService submissionService;

    /**
     * Constructs a new SubmissionsController with the required dependencies.
     * 
     * @param submissionService the service responsible for processing submissions;
     *                          must not be null
     */
    @Autowired
    public SubmissionsController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * Receives and processes a code submission for dynamic analysis.
     * 
     * <p>This endpoint accepts a Pub/Sub payload containing a base64-encoded ZIP file
     * with the code submission. The submission is processed through the following steps:</p>
     * <ol>
     *   <li>Extract and validate the submission ID from payload attributes</li>
     *   <li>Save the ZIP file with a unique filename based on submission ID</li>
     *   <li>Build Docker image for the analysis environment</li>
     *   <li>Run the submission in an isolated Docker container</li>
     *   <li>Collect test results and container output</li>
     *   <li>Publish results to Google Cloud Pub/Sub topic</li>
     *   <li>Clean up temporary files and resources</li>
     * </ol>
     * 
     * <p>The method supports concurrent processing - multiple submissions can be
     * processed simultaneously without conflicts due to unique file naming and
     * container isolation.</p>
     * 
     * @param payload the Pub/Sub payload containing the submission data.
     *                Must include:
     *                <ul>
     *                  <li>message.data: base64-encoded ZIP file</li>
     *                  <li>message.attributes.submissionId: unique submission identifier</li>
     *                </ul>
     * @return ResponseEntity containing:
     *         <ul>
     *           <li>200 OK: if submission was received and processing started successfully</li>
     *           <li>400 Bad Request: if submission data is invalid or processing fails</li>
     *         </ul>
     * @throws RuntimeException if there are unrecoverable errors during processing
     * @see PubSubPayload
     * @see SubmissionService#processSubmission(PubSubPayload)
     */
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
