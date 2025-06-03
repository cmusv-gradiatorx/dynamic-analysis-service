package edu.cmu.gradiatorx.dynamic.model;

import java.util.Map;

/**
 * Represents a Google Cloud Pub/Sub message structure.
 * This class maps to the standard Pub/Sub message format containing
 * message data, attributes, and metadata.
 *
 * @author Dynamic Analysis Service
 * @version 1.0
 * @since 1.0
 */
public class PubSubMessage {

    /**
     * The message data encoded as a base64 string.
     * Contains the actual payload of the message.
     */
    public String data;

    /**
     * A map of custom attributes associated with the message.
     * These attributes provide additional metadata about the message
     * such as submission ID, processing flags, etc.
     */
    public Map<String, String> attributes;

    /**
     * Unique identifier assigned to the message by the Pub/Sub service.
     * This ID is automatically generated when the message is published.
     */
    public String messageId;

    /**
     * RFC3339 timestamp representing when the message was published.
     * Format: "YYYY-MM-DDTHH:MM:SS.sssZ"
     */
    public String publishTime;
}
