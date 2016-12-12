package com.patientping.hiring.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class EntityWrapper<T> {
  public static final String ID_KEY = "id";
  public static final String TYPE_KEY = "type";
  public static final String ATTRIBUTES_KEY = "attributes";
  public static final String LINKS_KEY = "links";
  public static final String META_KEY = "meta";
  public static final String RELATIONSHIPS_KEY = "relationships";

  private final Object id;
  private final String type;
  private final T attributes;
  private final Map<String, Object> links;
  private final Map<String, Object> meta;
  private final ImmutableMap<String, JsonApiDocument<?>> relationships;

  @JsonCreator
  public EntityWrapper(@JsonProperty(ID_KEY) final Object id,
                       @JsonProperty(TYPE_KEY) final String type,
                       @JsonProperty(ATTRIBUTES_KEY) final T attributes,
                       @JsonProperty(LINKS_KEY) final Map<String, Object> links,
                       @JsonProperty(META_KEY) final Map<String, Object> meta,
                       @JsonProperty(RELATIONSHIPS_KEY) final Map<String, JsonApiDocument<?>> relationships) {
    this.id = id;
    this.type = type;
    this.attributes = attributes;
    this.links = links;
    this.meta = meta;
    this.relationships = relationships == null ? null : ImmutableMap.copyOf(relationships);
  }

  @JsonProperty(ID_KEY)
  public Object getId() {
    return id;
  }

  @JsonProperty(TYPE_KEY)
  public String getType() {
    return type;
  }

  @JsonProperty(ATTRIBUTES_KEY)
  public T getAttributes() {
    return attributes;
  }

  @JsonProperty(LINKS_KEY)
  public Map<String, Object> getLinks() {
    return links;
  }

  @JsonProperty(META_KEY)
  public Map<String, Object> getMeta() {
    return meta;
  }

  public Map<String, JsonApiDocument<?>> getRelationships() {
    return relationships;
  }
}
