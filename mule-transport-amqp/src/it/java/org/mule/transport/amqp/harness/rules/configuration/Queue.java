/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
    "name",
    "vhost",
    "durable",
    "auto_delete",
    "arguments"
})
public class Queue {

    @JsonProperty("name")
    private String name;

    @JsonProperty("vhost")
    private String vhost;

    @JsonProperty("durable")
    private boolean durable;

    @JsonProperty("auto_delete")
    private boolean autoDelete;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("vhost")
    public String getVhost() {
        return vhost;
    }

    @JsonProperty("vhost")
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    @JsonProperty("durable")
    public boolean isDurable() {
        return durable;
    }

    @JsonProperty("durable")
    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    @JsonProperty("auto_delete")
    public boolean isAutoDelete() {
        return autoDelete;
    }

    @JsonProperty("auto_delete")
    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
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
