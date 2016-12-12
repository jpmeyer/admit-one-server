package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class LinkSerializer<T> extends StdSerializer<T> implements Serializable, ContextualSerializer {
  private final Optional<BeanPropertyWriter> selfWriter;
  private final Optional<BeanPropertyWriter> relatedWriter;
  private final Optional<JsonSerializer<Object>> linkageSerializer;
  private Object beanContext;

  public LinkSerializer(final BeanProperty property, final BeanSerializerBuilder builder) {
    super(property.getType());
    selfWriter =
        StreamSupport.stream(property
            .getMember()
            .annotations()
            .spliterator(), false)
        .filter(a -> a instanceof Self)
        .<BeanPropertyWriter>map(a ->
            new TemplatedBeanPropertyWriter(((Self) a).value(), builder.getBeanDescription())
                .withName(JsonApiModule.SELF_PROPERTY_NAME))
        .findFirst();
    relatedWriter =
        StreamSupport.stream(property
            .getMember()
            .annotations()
            .spliterator(), false)
            .filter(a -> a instanceof Related)
            .<BeanPropertyWriter>map(a ->
                new TemplatedBeanPropertyWriter(((Related) a).value(), builder.getBeanDescription())
                    .withName(JsonApiModule.RELATED_PROPERTY_NAME))
            .findFirst();
    linkageSerializer = Optional.empty();
  }

  protected LinkSerializer(final LinkSerializer<T> base,
                           final Optional<JsonSerializer<Object>> linkageSerializer) {
    super(base.handledType());
    this.selfWriter = base.selfWriter;
    this.relatedWriter = base.relatedWriter;
    this.linkageSerializer = linkageSerializer;
  }

  @Override
  public void serialize(final T value, final JsonGenerator generator,
                        final SerializerProvider provider)
      throws IOException {
    generator.writeStartObject();
    generator.setCurrentValue(value);

    if (selfWriter.isPresent()) {
      try {
        selfWriter.get().serializeAsField(beanContext, generator, provider);
      } catch (final Exception ex) {
        wrapAndThrow(provider, ex, value, selfWriter.get().getName());
      }
    }

    if (relatedWriter.isPresent()) {
      try {
        relatedWriter.get().serializeAsField(beanContext, generator, provider);
      } catch (final Exception ex) {
        wrapAndThrow(provider, ex, value, relatedWriter.get().getName());
      }
    }

    if (linkageSerializer.isPresent()) {
      generator.writeFieldName(JsonApiModule.LINKAGE_FIELD_NAME);
      linkageSerializer.get().serialize(value, generator, provider);
    }

    generator.writeEndObject();
  }

  @Override
  public JsonSerializer<?> createContextual(final SerializerProvider provider,
                                            final BeanProperty property)
      throws JsonMappingException {
    return new LinkSerializer<>(this, StreamSupport.stream(property
        .getMember()
        .annotations()
        .spliterator(), false)
        .filter(a -> a instanceof Link && ((Link) a).linkage())
        .findFirst()
        .map(a -> getSerializer(provider, property)));
  }

  public void setBeanContext(final Object beanContext) {
    this.beanContext = beanContext;
  }

  private static JsonSerializer<Object> getSerializer(final SerializerProvider provider,
                                                      final BeanProperty property) {
    try {
      return provider.findValueSerializer(property.getType(), property);
    } catch (final JsonMappingException jme) {
      throw new RuntimeException(jme);
    }
  }
}
