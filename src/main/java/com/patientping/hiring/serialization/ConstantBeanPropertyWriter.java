package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;

final class ConstantBeanPropertyWriter<T> extends VirtualBeanPropertyWriter {
  private final T value;

  @SuppressWarnings("unchecked")
  public ConstantBeanPropertyWriter(final JsonSerializer<? super T> serializer, final T value) {
    this._serializer = (JsonSerializer<Object>) serializer;
    this.value = value;
  }

  public ConstantBeanPropertyWriter(final ConstantBeanPropertyWriter<T> base, final PropertyName name) {
    super(base, name);
    this.value = base.value;
  }

  @Override
  protected T value(final Object bean, final JsonGenerator jgen, final SerializerProvider prov) throws Exception {
    return value;
  }

  @Override
  public ConstantBeanPropertyWriter withConfig(final MapperConfig<?> config,
                                               final AnnotatedClass declaringClass,
                                               final BeanPropertyDefinition property,
                                               final JavaType type) {
    return this;
  }

  public ConstantBeanPropertyWriter<T> withName(final PropertyName name) {
    return new ConstantBeanPropertyWriter<>(this, name);
  }
}
