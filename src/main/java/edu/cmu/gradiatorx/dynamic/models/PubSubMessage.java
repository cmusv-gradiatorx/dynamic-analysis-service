package edu.cmu.gradiatorx.dynamic.models;

import java.util.Map;

public class PubSubMessage {
    public String data;
    public Map<String, String> attributes;
    public String messageId;
    public String publishTime;
}
