package com.patientping.hiring.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.Annotations;

import static com.patientping.hiring.serialization.JsonApiModule.*;

public class JsonApiSerializerModifier extends BeanSerializerModifier {

  @Override
  public BeanSerializerBuilder updateBuilder(final SerializationConfig config,
                                             final BeanDescription beanDescription,
                                             final BeanSerializerBuilder builder) {
    final JavaType type = beanDescription.getType();
    final Annotations classAnnotations = beanDescription.getClassAnnotations();
    final Entity entityAnnotation = classAnnotations.get(Entity.class);
    if (entityAnnotation != null) {
      final EntitySerializer<?> entitySerializer = new EntitySerializer<>(type,
          getIdWriter(type, builder),
          getTypeWriter(type, builder),
          constructAttributesSerializer(type, builder),
          constructMetaSerializer(type, builder),
          constructLinksSerializer(type, builder),
          builder);
      return new BeanSerializerBuilder(beanDescription) {
        @Override
        public JsonSerializer<?> build() {
          return entitySerializer;
        }
      };
    }
    return builder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public JsonSerializer<?> modifySerializer(final SerializationConfig config, final BeanDescription beanDescription, final JsonSerializer<?> serializer) {
    if (JsonApiDocument.class.isAssignableFrom(beanDescription.getBeanClass())) {
      return new JsonApiDocument.Serializer<>((JsonSerializer<JsonApiDocument<Object>>) serializer);
    }
    return serializer;
  }
}
