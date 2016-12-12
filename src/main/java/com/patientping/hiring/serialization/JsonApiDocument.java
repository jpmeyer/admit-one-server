package com.patientping.hiring.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.patientping.hiring.util.UnsafeLinkedHashMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class JsonApiDocument<T> {
  public static final String DATA_KEY = "data";
  private static final ThreadLocal<JsonApiDocument<?>> includedLocal = new ThreadLocal<>();
  private final T data;
  private final Set<Object> included = Collections.newSetFromMap(new UnsafeLinkedHashMap<>());

  @JsonCreator
  public JsonApiDocument(@JsonProperty(DATA_KEY) final T data) {
    this.data = data;
  }

  @JsonProperty(DATA_KEY)
  public T getData() {
    return data;
  }

  @JsonProperty
  public Set<Object> getIncluded() {
    return included;
  }

  protected static void include(final Object entity) {
    final JsonApiDocument<?> document = includedLocal.get();
    if (document != null) {
      if(document.getData() != null) {
        if(document.getData().equals(entity)) {
          return;
        }
        if(document.getData() instanceof Iterable<?>) {
          for(final Object d: (Iterable<?>)document.getData()) {
            if(entity.equals(d)) {
              return;
            }
          }
        } else if (document.getData() instanceof Object[]) {
          for(final Object d: (Object[])document.getData()) {
            if(entity.equals(d)) {
              return;
            }
          }
        }
      }
      document.included.add(entity);
    }
  }

  protected static final class Serializer<T>
      extends StdSerializer<JsonApiDocument<T>>
      implements Serializable {
    private static final long serialVersionUID = -2594929346413588404L;
    private final JsonSerializer<JsonApiDocument<T>> base;

    public Serializer(JsonSerializer<JsonApiDocument<T>> base) {
      super(base.handledType());
      this.base = base;
    }

    @Override
    public void serialize(final JsonApiDocument<T> value,
                          final JsonGenerator generator,
                          final SerializerProvider provider)
        throws IOException {
      includedLocal.set(value);
      base.serialize(value, generator, provider);
      includedLocal.set(null);
    }
  }
}
