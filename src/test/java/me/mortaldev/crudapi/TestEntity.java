package me.mortaldev.crudapi;

import java.util.Objects;

/** Plain entity readable by both Jackson (public fields, no-arg constructor) and Gson. */
public class TestEntity implements CRUD.Identifiable {
  public String id;
  public String name;
  public int level;

  public TestEntity() {}

  public TestEntity(String id, String name, int level) {
    this.id = id;
    this.name = name;
    this.level = level;
  }

  public static TestEntity create(String id) {
    return new TestEntity(id, "Unknown", 1);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TestEntity other
        && Objects.equals(id, other.id)
        && Objects.equals(name, other.name)
        && level == other.level;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, level);
  }

  @Override
  public String toString() {
    return "TestEntity[" + id + ", " + name + ", " + level + "]";
  }
}
