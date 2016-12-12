package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Throwables;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonApiModule extends SimpleModule implements Serializable {
  private static final long serialVersionUID = -1021557591227015308L;

  static final String ID_KEY = "id";
  static final String TYPE_KEY = "type";

  static final PropertyName ID_NAME = new PropertyName(ID_KEY);
  static final PropertyName TYPE_NAME = new PropertyName(TYPE_KEY);
  static final PropertyName SELF_PROPERTY_NAME = new PropertyName("self");
  static final PropertyName RELATED_PROPERTY_NAME = new PropertyName("related");

  static final SerializedString ATTR_FIELD_NAME = new SerializedString("attributes");
  static final SerializedString META_FIELD_NAME = new SerializedString("meta");
  static final SerializedString LINKS_FIELD_NAME = new SerializedString("links");
  static final SerializedString LINKAGE_FIELD_NAME = new SerializedString("linkage");

  static final Converter<String, String> CLASS_NAME_CONVERTER =
      CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN);

  @Override
  public String getModuleName() {
    return getClass().getSimpleName();
  }

  @Override
  public Version version() {
    return new Version(1, 0, 0, null, "com.patientping.hiring", "json-api");
  }

  @Override
  public void setupModule(final SetupContext context) {
    context.addBeanSerializerModifier(new JsonApiSerializerModifier());
  }

  protected static BeanPropertyWriter getIdWriter(final JavaType type, final BeanSerializerBuilder builder) {
    return Optional.ofNullable(builder
        .getBeanDescription()
        .getClassAnnotations()
        .get(Id.class))
        .<BeanPropertyWriter>map(id ->
            new TemplatedBeanPropertyWriter(id.value(), builder.getBeanDescription()).withName(ID_NAME))
        .orElseGet(() -> builder
            .getProperties()
            .stream()
            .filter(p -> ID_KEY.equals(p.getName()))
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("Missing property id on entity: " + type.toString()))
        );
  }

  protected static BeanPropertyWriter getTypeWriter(final JavaType type, final BeanSerializerBuilder builder) {
    return Optional.ofNullable(builder
        .getBeanDescription()
        .getClassAnnotations()
        .get(Type.class))
        .<BeanPropertyWriter>map(t ->
            new TemplatedBeanPropertyWriter(t.value(), builder.getBeanDescription()).withName(TYPE_NAME))
        .orElseGet(() -> builder
            .getProperties()
            .stream()
            .filter(p -> TYPE_KEY.equals(p.getName()))
            .findFirst()
            .orElseGet(() ->
                new ConstantBeanPropertyWriter<>(new ToStringSerializer(),
                    CLASS_NAME_CONVERTER.convert(type.getRawClass().getSimpleName()))
                    .withName(TYPE_NAME)
            )
        );
  }

  protected static BeanSerializer constructMetaSerializer(final JavaType type, final BeanSerializerBuilder builder) {
    return new BeanSerializer(type, builder,
        builder
            .getProperties()
            .stream()
            .filter(JsonApiModule::isMetaWriter)
            .toArray(BeanPropertyWriter[]::new),
        builder.getFilteredProperties() == null ? null :
            Arrays.stream(builder.getFilteredProperties())
                .filter(JsonApiModule::isMetaWriter)
                .toArray(BeanPropertyWriter[]::new));
  }

  protected static boolean isMetaWriter(final BeanPropertyWriter writer) {
    return StreamSupport
        .stream(writer.getMember().annotations().spliterator(), false)
        .anyMatch(a -> a instanceof Meta);
  }

  protected static BeanSerializer constructLinksSerializer(final JavaType type, final BeanSerializerBuilder builder) {
    final Optional<TemplatedBeanPropertyWriter> selfLink = StreamSupport
        .stream(builder
            .getClassInfo()
            .annotations()
            .spliterator(), false)
        .filter(a -> a instanceof Self)
        .findFirst()
        .map(a ->
            new TemplatedBeanPropertyWriter(((Self) a).value(), builder.getBeanDescription())
                .withName(SELF_PROPERTY_NAME));
    final Predicate<BeanPropertyWriter> selfOverride =
        (p -> !(selfLink.isPresent() && "self".equals(p.getName())));
    return new BeanSerializer(type, builder,
        Stream.concat(selfLink.map(Stream::of).orElse(Stream.empty()),
            builder.getProperties()
                .stream()
                .filter(JsonApiModule::isLinkWriter)
                .filter(selfOverride))
                .map(p -> mapLinks(p, builder))
                .toArray(BeanPropertyWriter[]::new),
        builder.getFilteredProperties() == null ? null :
            Stream.concat(selfLink.map(Stream::of).orElse(Stream.empty()),
                Arrays.stream(builder.getFilteredProperties())
                    .filter(JsonApiModule::isLinkWriter)
                    .filter(selfOverride)
                    .map(p -> mapLinks(p, builder)))
                    .toArray(BeanPropertyWriter[]::new)) {
      @Override
      public JsonSerializer<?> createContextual(final SerializerProvider provider, final BeanProperty property) throws JsonMappingException {
        Arrays.stream(this._props)
            .filter(p -> p instanceof LinkPropertyWriter)
            .map(p -> (LinkPropertyWriter)p)
            .forEach(w -> w.contextualize(provider));
        return super.createContextual(provider, property);
      }

      @Override
      protected void serializeFields(final Object bean, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        for (final BeanPropertyWriter writer: _props) {
          final JsonSerializer<Object> serializer = writer.getSerializer();
          if (serializer instanceof LinkSerializer) {
            ((LinkSerializer)serializer).setBeanContext(bean);
          }
        }
        super.serializeFields(bean, jgen, provider);
      }

      @Override
      protected void serializeFieldsFiltered(final Object bean, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        for (final BeanPropertyWriter writer: _props) {
          final JsonSerializer<Object> serializer = writer.getSerializer();
          if (serializer instanceof LinkSerializer) {
            ((LinkSerializer)serializer).setBeanContext(bean);
          }
        }
        super.serializeFieldsFiltered(bean, jgen, provider);
      }
    };
  }

  protected static boolean isLinkWriter(final BeanPropertyWriter writer) {
    return StreamSupport
        .stream(writer.getMember().annotations().spliterator(), false)
        .anyMatch(a -> a instanceof Link);
  }

  protected static BeanSerializer constructAttributesSerializer(final JavaType type,
                                                                final BeanSerializerBuilder builder) {
    return new BeanSerializer(type, builder,
        builder
            .getProperties()
            .stream()
            .filter(JsonApiModule::isAttributeWriter)
            .toArray(BeanPropertyWriter[]::new),
        builder.getFilteredProperties() == null ? null :
            Arrays.stream(builder.getFilteredProperties())
                .filter(JsonApiModule::isAttributeWriter)
                .toArray(BeanPropertyWriter[]::new));
  }

  protected static boolean isAttributeWriter(final BeanPropertyWriter writer) {
    return !(ID_KEY.equals(writer.getName()) ||
        TYPE_KEY.equals(writer.getName()) ||
        StreamSupport
            .stream(writer.getMember().annotations().spliterator(), false)
            .anyMatch(a -> a instanceof Link || a instanceof Meta));
  }

  protected static BeanPropertyWriter mapLinks(final BeanPropertyWriter base, final BeanSerializerBuilder builder) {
    return !base.isVirtual() && StreamSupport
        .stream(base.getMember().annotations().spliterator(), false)
        .anyMatch(a -> a instanceof Link && !((Link)a).useNativeSerializer()) ?
        new LinkPropertyWriter(base, new LinkSerializer<>(base, builder)) : base;
  }

  @SuppressWarnings("unchecked")
  private static class LinkPropertyWriter extends BeanPropertyWriter {
    public LinkPropertyWriter(final BeanPropertyWriter base, final LinkSerializer<Object> serializer) {
      super(base);
      _serializer = serializer;
    }

    public void contextualize(final SerializerProvider provider) {
      try {
        if (_serializer instanceof LinkSerializer) {
          _serializer = (JsonSerializer<Object>) ((LinkSerializer<?>) _serializer).createContextual(provider, this);
        }
      } catch (final JsonMappingException jme) {
        throw Throwables.propagate(jme);
      }
    }
  }
}
