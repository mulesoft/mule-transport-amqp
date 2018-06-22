/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness.rules.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "queues",
    "exchanges",
    "bindings"
})
public class Configuration {

    @JsonProperty("queues")
    private List<Queue> queues = new ArrayList<Queue>();
    
    @JsonProperty("exchanges")
    private List<Exchange> exchanges = new ArrayList<Exchange>();
    
    @JsonProperty("bindings")
    private List<Binding> bindings = new ArrayList<Binding>();
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("queues")
    public List<Queue> getQueues() {
        return queues;
    }

    @JsonProperty("queues")
    public void setQueues(List<Queue> queues) {
        this.queues = queues;
    }

    @JsonProperty("exchanges")
    public List<Exchange> getExchanges() {
        return exchanges;
    }

    @JsonProperty("exchanges")
    public void setExchanges(List<Exchange> exchanges) {
        this.exchanges = exchanges;
    }

    @JsonProperty("bindings")
    public List<Binding> getBindings() {
        return bindings;
    }

    @JsonProperty("bindings")
    public void setBindings(List<Binding> bindings) {
        this.bindings = bindings;
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
