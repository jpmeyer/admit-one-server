package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.StreamSupport;

import static com.patientping.hiring.serialization.JsonApiModule.ATTR_FIELD_NAME;
import static com.patientping.hiring.serialization.JsonApiModule.LINKS_FIELD_NAME;
import static com.patientping.hiring.serialization.JsonApiModule.META_FIELD_NAME;

public class EntitySerializer<T> extends StdSerializer<T> implements Serializable, ContextualSerializer {
  private static final long serialVersionUID = 3791175974701193072L;

  private final BeanPropertyWriter idWriter;
  private final BeanPropertyWriter typeWriter;
  private final JsonSerializer<T> attributesSerializer;
  private final JsonSerializer<T> metaSerializer;
  private final JsonSerializer<T> linksSerializer;
  private final BeanSerializerBuilder builder;

  public EntitySerializer(final JavaType type,
                          final BeanPropertyWriter idWriter,
                          final BeanPropertyWriter typeWriter,
                          final JsonSerializer<T> attributesSerializer,
                          final JsonSerializer<T> metaSerializer,
                          final JsonSerializer<T> linksSerializer,
                          final BeanSerializerBuilder builder) {
    super(type);
    this.idWriter = idWriter;
    this.typeWriter = typeWriter;
    this.attributesSerializer = attributesSerializer;
    this.metaSerializer = metaSerializer;
    this.linksSerializer = linksSerializer;
    this.builder = builder;
  }

  public EntitySerializer(final Class<T> type,
                          final BeanPropertyWriter idWriter,
                          final BeanPropertyWriter typeWriter,
                          final JsonSerializer<T> attributesSerializer,
                          final JsonSerializer<T> metaSerializer,
                          final JsonSerializer<T> linksSerializer,
                          final BeanSerializerBuilder builder) {
    super(type);
    this.idWriter = idWriter;
    this.typeWriter = typeWriter;
    this.attributesSerializer = attributesSerializer;
    this.metaSerializer = metaSerializer;
    this.linksSerializer = linksSerializer;
    this.builder = builder;
  }

  @Override
  public final void serialize(
      final T bean,
      final JsonGenerator generator,
      final SerializerProvider provider)
      throws IOException
  {
    generator.writeStartObject();
    generator.setCurrentValue(bean);

    try {
      idWriter.serializeAsField(bean, generator, provider);
    } catch (final Exception ex) {
      wrapAndThrow(provider, ex, bean, idWriter.getName());
    }

    try {
      typeWriter.serializeAsField(bean, generator, provider);
    } catch (final Exception ex) {
      wrapAndThrow(provider, ex, bean, typeWriter.getName());
    }

    if (!metaSerializer.isEmpty(provider, bean)) {
      generator.writeFieldName(META_FIELD_NAME);
      metaSerializer.serialize(bean, generator, provider);
    }

    if (!attributesSerializer.isEmpty(provider, bean)) {
      generator.writeFieldName(ATTR_FIELD_NAME);
      attributesSerializer.serialize(bean, generator, provider);
    }

    if (!linksSerializer.isEmpty(provider, bean)) {
      generator.writeFieldName(LINKS_FIELD_NAME);
      linksSerializer.serialize(bean, generator, provider);
    }

    generator.writeEndObject();
  }

  @Override public String toString() {
    return "EntitySerializer for " + handledType().getName();
  }

  @Override
  public JsonSerializer<?> createContextual(final SerializerProvider provider, final BeanProperty property) throws JsonMappingException {
    //TODO: implement entity link serializer.
    return property == null ? contextualize(provider) : StreamSupport
        .stream(property.getMember().annotations().spliterator(), false)
        .filter(a -> a instanceof Link && !((Link)a).useNativeSerializer())
        .findFirst()
        .<JsonSerializer<?>>map(a -> new LinkageSerializer<>(property.getType(), builder))
        .orElse(contextualize(provider));
  }

  @SuppressWarnings("unchecked")
  private EntitySerializer<T> contextualize(final SerializerProvider provider) throws JsonMappingException {
    return new EntitySerializer<T>(
        this.handledType(),
        this.idWriter,
        this.typeWriter,
        this.attributesSerializer,
        this.metaSerializer,
        this.linksSerializer instanceof ContextualSerializer ? (JsonSerializer<T>)((ContextualSerializer)linksSerializer).createContextual(provider, null) : linksSerializer,
        this.builder);
  }
}
