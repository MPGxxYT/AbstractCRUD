package me.mortaldev.crudapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import me.mortaldev.abstractcrud.crudapi.CRUD;
import me.mortaldev.abstractcrud.crudapi.codec.Codec;
import me.mortaldev.abstractcrud.crudapi.codec.GsonCodec;
import me.mortaldev.abstractcrud.crudapi.codec.JacksonCodec;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CRUDTest {

  @TempDir Path dir;

  static Stream<Codec> codecs() {
    return Stream.of(new JacksonCodec(), new GsonCodec());
  }

  private CRUD<TestEntity> crud(Codec codec) {
    return new CRUD<>(TestEntity.class, dir.resolve("entities"), codec);
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void saveThenGetReturnsEqualValue(Codec codec) {
    CRUD<TestEntity> crud = crud(codec);
    TestEntity entity = new TestEntity("a", "Alice", 7);

    assertTrue(crud.save("a", entity));

    assertEquals(Optional.of(entity), crud.get("a"));
    assertTrue(Files.isRegularFile(dir.resolve("entities/a.json")));
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void saveReplacesAndLeavesNoTempFile(Codec codec) throws IOException {
    CRUD<TestEntity> crud = crud(codec);
    crud.save("a", new TestEntity("a", "Old", 1));
    crud.save("a", new TestEntity("a", "New", 2));

    assertEquals("New", crud.get("a").orElseThrow().name);
    try (Stream<Path> files = Files.list(dir.resolve("entities"))) {
      assertEquals(1, files.count());
    }
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void getMissingIdIsEmpty(Codec codec) {
    assertEquals(Optional.empty(), crud(codec).get("missing"));
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void corruptFileReadsAsEmptyAndIsSkippedByLoadAll(Codec codec) throws IOException {
    CRUD<TestEntity> crud = crud(codec);
    crud.save("good", new TestEntity("good", "Good", 1));
    Files.writeString(dir.resolve("entities/bad.json"), "{ not json");

    assertEquals(Optional.empty(), crud.get("bad"));
    assertEquals(Map.of("good", new TestEntity("good", "Good", 1)), crud.loadAll());
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void deleteRemovesFileAndReportsWhetherOneExisted(Codec codec) {
    CRUD<TestEntity> crud = crud(codec);
    crud.save("a", TestEntity.create("a"));

    assertTrue(crud.delete("a"));
    assertFalse(crud.delete("a"));
    assertEquals(Optional.empty(), crud.get("a"));
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void loadAllCreatesMissingDirectory(Codec codec) {
    CRUD<TestEntity> crud = crud(codec);

    assertEquals(Map.of(), crud.loadAll());
    assertTrue(Files.isDirectory(crud.directory()));
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void loadAllIgnoresOtherFilesAndKeysByFileName(Codec codec) throws IOException {
    CRUD<TestEntity> crud = crud(codec);
    crud.save("a.json.b", new TestEntity("a.json.b", "Dotted", 3));
    Files.writeString(dir.resolve("entities/notes.txt"), "ignored");
    Files.createDirectories(dir.resolve("entities/sub.json"));

    Map<String, TestEntity> all = crud.loadAll();

    assertEquals(Map.of("a.json.b", new TestEntity("a.json.b", "Dotted", 3)), all);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", ".", "..", "a/b", "a\\b", "../escape"})
  void invalidIdsAreRejected(String id) {
    CRUD<TestEntity> crud = crud(new GsonCodec());

    assertFalse(crud.save(id, TestEntity.create("x")));
    assertEquals(Optional.empty(), crud.get(id));
    assertFalse(crud.delete(id));
    assertFalse(Files.exists(dir.resolve("escape.json")));
  }

  @ParameterizedTest
  @MethodSource("codecs")
  void nullValueIsNotSaved(Codec codec) {
    CRUD<TestEntity> crud = crud(codec);

    assertFalse(crud.save("a", null));
    assertEquals(Optional.empty(), crud.get("a"));
  }
}
