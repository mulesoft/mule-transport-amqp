/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness.rules.configuration;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "source",
    "vhost",
    "destination",
    "destination_type",
    "routing_key",
    "arguments"
})
public class Binding {

    @JsonProperty("source")
    private String source;
    
    @JsonProperty("vhost")
    private String vhost;
    
    @JsonProperty("destination")
    private String destination;
    
    @JsonProperty("destination_type")
    private String destinationType;
    
    @JsonProperty("routing_key")
    private String routingKey;
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("source")
    public String getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(String source) {
        this.source = source;
    }

    @JsonProperty("vhost")
    public String getVhost() {
        return vhost;
    }

    @JsonProperty("vhost")
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    @JsonProperty("destination")
    public String getDestination() {
        return destination;
    }

    @JsonProperty("destination")
    public void setDestination(String destination) {
        this.destination = destination;
    }

    @JsonProperty("destination_type")
    public String getDestinationType() {
        return destinationType;
    }

    @JsonProperty("destination_type")
    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    @JsonProperty("routing_key")
    public String getRoutingKey() {
        return routingKey;
    }

    @JsonProperty("routing_key")
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
