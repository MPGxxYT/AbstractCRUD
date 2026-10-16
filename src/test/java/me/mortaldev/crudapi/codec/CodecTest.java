package me.mortaldev.crudapi.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import me.mortaldev.abstractcrud.crudapi.codec.Codec;
import me.mortaldev.abstractcrud.crudapi.codec.GsonCodec;
import me.mortaldev.abstractcrud.crudapi.codec.JacksonCodec;
import me.mortaldev.crudapi.TestEntity;
import me.mortaldev.abstractcrud.typeadapters.OptionalTypeAdapter;
import org.junit.jupiter.api.Test;

class CodecTest {

  /** What 1.x Jackson.saveJsonObject wrote: writerWithDefaultPrettyPrinter on a default mapper. */
  private static final String JACKSON_1X_FILE =
      "{\r\n  \"id\" : \"p1\",\r\n  \"name\" : \"Steve\",\r\n  \"level\" : 4\r\n}";

  /** What 1.x GSON.saveJsonObject wrote: GsonBuilder().setPrettyPrinting(). */
  private static final String GSON_1X_FILE =
      "{\n  \"id\": \"p1\",\n  \"name\": \"Steve\",\n  \"level\": 4\n}";

  private static final TestEntity STEVE = new TestEntity("p1", "Steve", 4);

  @Test
  void jacksonReadsFilesWrittenBy1x() throws IOException {
    assertEquals(STEVE, new JacksonCodec().decode(JACKSON_1X_FILE, TestEntity.class));
  }

  @Test
  void gsonReadsFilesWrittenBy1x() throws IOException {
    assertEquals(STEVE, new GsonCodec().decode(GSON_1X_FILE, TestEntity.class));
  }

  @Test
  void gsonWritesSameTextAs1x() throws IOException {
    assertEquals(GSON_1X_FILE, new GsonCodec().encode(STEVE));
  }

  @Test
  void jacksonWritesPrettyPrintedJson() throws IOException {
    String json = new JacksonCodec().encode(STEVE);
    assertTrue(json.contains("\n  \"name\" : \"Steve\""), json);
  }

  @Test
  void decodeFailuresAreIOExceptions() {
    assertThrows(IOException.class, () -> new JacksonCodec().decode("{", TestEntity.class));
    assertThrows(IOException.class, () -> new GsonCodec().decode("{", TestEntity.class));
  }

  public static class Dated {
    public LocalDate date;
  }

  @Test
  void jacksonConfigureCallbackRegistersModules() throws IOException {
    Codec codec = new JacksonCodec(mapper -> mapper.registerModule(new JavaTimeModule()));
    Dated dated = new Dated();
    dated.date = LocalDate.of(2026, 10, 14);

    assertEquals(dated.date, codec.decode(codec.encode(dated), Dated.class).date);
  }

  static class Nicknamed {
    Optional<String> nickname = Optional.empty();
  }

  @Test
  void gsonConfigureCallbackRegistersOptionalFactory() throws IOException {
    Codec codec = new GsonCodec(b -> b.registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
    Nicknamed value = new Nicknamed();
    value.nickname = Optional.of("Steve");

    assertEquals(Optional.of("Steve"), codec.decode(codec.encode(value), Nicknamed.class).nickname);
  }
}
