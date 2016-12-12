package com.patientping.hiring.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.patientping.hiring.util.AbstractMap;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

import java.util.Map;
import java.util.stream.Collectors;

final class TemplatedBeanPropertyWriter extends VirtualBeanPropertyWriter {
  private final String template;
  private final Map<String, AnnotatedMember> members;

  public TemplatedBeanPropertyWriter(final String template, final BeanDescription beanDescription) {
    this.template = template;
    this.members = beanDescription
        .findProperties()
        .stream()
        .map(p -> new AbstractMap.SimpleImmutableEntry<>(p.getName(), p.getAccessor()))
        .filter(e -> e.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    this._serializer = new ToStringSerializer();

  }

  @Override
  protected Object value(final Object bean,
                         final JsonGenerator jgen,
                         final SerializerProvider prov)
      throws Exception {
    final StringTemplate stringTemplate = new StringTemplate(template, AngleBracketTemplateLexer.class);
    for(final Map.Entry<String, AnnotatedMember> entry: members.entrySet()) {
      stringTemplate.setAttribute(entry.getKey(), entry.getValue().getValue(bean));
    }
    return stringTemplate.toString();
  }

  @Override
  public TemplatedBeanPropertyWriter withConfig(final MapperConfig<?> config,
                                                final AnnotatedClass declaringClass,
                                                final BeanPropertyDefinition property,
                                                final JavaType type) {
    return this;
  }

  public TemplatedBeanPropertyWriter(final TemplatedBeanPropertyWriter base, final PropertyName name) {
    super(base, name);
    this.template = base.template;
    this.members = base.members;
  }

  public TemplatedBeanPropertyWriter withName(final PropertyName name) {
    return new TemplatedBeanPropertyWriter(this, name);
  }
}
