package edu.cmu.gradiatorx.dynamic.models;

/**
 * Represents the complete payload structure received from Google Cloud Pub/Sub.
 * This is the top-level container that wraps a PubSubMessage along with
 * subscription information when receiving push notifications.
 * 
 * <p>This class is typically used as the request body when Pub/Sub sends
 * push notifications to HTTP endpoints.</p>
 * 
 * @author Dynamic Analysis Service
 * @version 1.0
 * @since 1.0
 */
public class PubSubPayload {
    
    /**
     * The actual Pub/Sub message containing data, attributes, and metadata.
     * This is the core message that was published to the topic.
     */
    public PubSubMessage message;
    
    /**
     * The name of the subscription that received this message.
     * Format: "projects/{project-id}/subscriptions/{subscription-name}"
     */
    public String subscription;
}
