package com.patientping.hiring.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JsonApiModuleTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JsonApiModule());
  @Test
  public void testSimpleEntity() throws IOException {
    System.out.println(mapper.writeValueAsString(new SimpleEntity()));
  }

  @Test
  public void testSimpleEntity2() throws IOException {
    System.out.println(mapper.writeValueAsString(new SimpleEntity2()));
  }

  @Test
  public void testSimpleEntity3() throws IOException {
    System.out.println(mapper.writeValueAsString(new SimpleEntity3()));
  }

  @Test
  public void testParentEntity() throws IOException {
    System.out.println(mapper.writeValueAsString(new ToOneEntity()));
  }

  @Test
  public void testJsonApiDocumentWithOne() throws IOException {
    System.out.println(mapper.writeValueAsString(new JsonApiDocument<>(new ToOneEntity())));
  }

  @Test
  public void testJsonApiDocumentWithMany() throws IOException {
    System.out.println(mapper.writeValueAsString(new JsonApiDocument<>(new ToManyEntity())));
  }

  @Test
  public void testJsonApiDocumentRecursiveIncludes() throws IOException {
    System.out.println(mapper.writeValueAsString(new JsonApiDocument<>(new RecursiveEntity())));
  }


  @Entity
  @Self("/v1/test/simple-entity/<id>")
  public static class SimpleEntity {
    public int id = 9001;
    public String type = "simple-entity";

    public String name = "HAL";

    @Meta
    public String version = "1.0";

    @Link(useNativeSerializer = true)
    public String parent = "test";
  }

  @Entity
  @Self("/v1/test/simple-entity-2/<id>")
  @Id("entity:<id>")
  public static class SimpleEntity2 {
    public int id = 9002;

    public String name = "HAL";

    @Meta
    public String version = "2.0";

    @Link(useNativeSerializer = true)
    public String parent = "test";
  }

  @Entity
  @Self("/v1/test/simple-entity-3/<id>")
  @Type("simple-entity-3")
  public static class SimpleEntity3 {
    public int id = 9003;

    public String name = "HAL";

    @Meta
    public String version = "3.0";

    @Link(useNativeSerializer = true)
    public String parent = "test";
  }
  
  @Entity
  @Self("/v1/test/to-one/<id>")
  @Type("to-one")
  public static class ToOneEntity {
    public int id = 100;
    
    public String name = "Mom";

    @Meta
    public String version = "1.0";

    @Link
    public ChildEntity child = new ChildEntity();
  }

  @Entity
  @Self("/v1/test/to-many/<id>")
  @Type("to-many")
  public static class ToManyEntity {
    public int id = 100;

    public String name = "Mom";

    @Meta
    public String version = "1.0";

    @Link
    public List<ChildEntity> children = ImmutableList.of(new ChildEntity(101), new ChildEntity(102));

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ToManyEntity that = (ToManyEntity) o;

      return id == that.id;

    }

    @Override
    public int hashCode() {
      return id;
    }
  }

  @Entity
  @Self("/v1/test/child/<id>")
  @Type("child")
  public static class ChildEntity {
    public ChildEntity() {
    }

    public ChildEntity(final int id) {
      this.id = id;
    }

    public int id = 101;

    public String name = "Son";

    @Meta
    public String version = "1.0";

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ChildEntity that = (ChildEntity) o;

      return id == that.id;

    }

    @Override
    public int hashCode() {
      return id;
    }
  }

  @Entity
  @Self("/v1/test/recursive/<id>")
  @Type("recursive")
  public static class RecursiveEntity {
    public int id = 7;

    public String name = "Grandparent";

    @Meta
    public String version = "1.0";

    @Link
    @Self("/v1/test/recursive/<id>/children")
    public List<ToManyEntity> children = ImmutableList.of(new ToManyEntity(), new ToManyEntity());
  }
}