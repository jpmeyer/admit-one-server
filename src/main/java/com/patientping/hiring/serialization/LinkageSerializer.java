package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.Serializable;

public class LinkageSerializer<T> extends StdSerializer<T> implements Serializable {
  private static final long serialVersionUID = 8965485452539015471L;

  private final BeanPropertyWriter idWriter;
  private final BeanPropertyWriter typeWriter;

  public LinkageSerializer(final JavaType type, final BeanSerializerBuilder builder) {
    super(type);
    idWriter = JsonApiModule.getIdWriter(type, builder);
    typeWriter = JsonApiModule.getTypeWriter(type, builder);
  }

  @Override
  public void serialize(
      final T bean,
      final JsonGenerator generator,
      final SerializerProvider provider)
      throws IOException {

    JsonApiDocument.include(bean);

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

    generator.writeEndObject();
  }
}
