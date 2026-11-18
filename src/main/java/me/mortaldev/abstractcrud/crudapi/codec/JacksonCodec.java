package me.mortaldev.abstractcrud.crudapi.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link Codec} that uses Jackson and writes pretty-printed JSON.
 *
 * <h2>What Jackson saves</h2>
 *
 * <ul>
 *   <li>Public fields, and every property with a public getter ({@code getName()}, {@code
 *       isActive()}). A private field with no getter is <b>not saved</b>, and no error is shown.
 *   <li>To read an object back, Jackson needs a no-argument constructor (it may be private), a
 *       constructor annotated with {@code @JsonCreator}, or a record. Values are then set through
 *       setters or public fields.
 *   <li>Fields or getters annotated with {@code @JsonIgnore} are skipped.
 *   <li>By default, a file containing a property the class does not have fails to read. Turn this
 *       off with {@code DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES} if you rename or remove
 *       fields.
 * </ul>
 *
 * <h2>Settings and custom serializers</h2>
 *
 * <p>Configure the {@link ObjectMapper} in the constructor callback. The mapper is built once,
 * when the codec is created:
 *
 * <pre>{@code
 * Codec codec = new JacksonCodec(mapper -> {
 *     mapper.registerModule(new SimpleModule()
 *         .addSerializer(Location.class, new LocationSerializer())
 *         .addDeserializer(Location.class, new LocationDeserializer()));
 *     mapper.registerModule(new JavaTimeModule());   // LocalDate, Instant, ...
 *     mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
 * });
 * }</pre>
 *
 * <p>Instances are safe to share between threads and between CRUDs.
 *
 * @see GsonCodec
 */
public final class JacksonCodec implements Codec {
  private final ObjectMapper mapper;
  private final ObjectWriter writer;

  /** Creates a codec with Jackson's default settings and no custom serializers. */
  public JacksonCodec() {
    this(mapper -> {});
  }

  /**
   * Creates a codec and lets you configure its {@link ObjectMapper} first: register modules and
   * serializers, or change features.
   *
   * @param configure called once, right away, with the new mapper. Don't keep a reference to the
   *     mapper or change it later; the codec is not designed for changes after construction.
   * @throws NullPointerException if {@code configure} is {@code null}
   */
  public JacksonCodec(Consumer<ObjectMapper> configure) {
    Objects.requireNonNull(configure, "configure");
    this.mapper = new ObjectMapper();
    configure.accept(mapper);
    this.writer = mapper.writerWithDefaultPrettyPrinter();
  }

  /**
   * Converts an object to pretty-printed JSON with Jackson.
   *
   * @param value the object to convert
   * @return the JSON text
   * @throws IOException if Jackson cannot serialize the object, for example a field type with no
   *     serializer
   */
  @Override
  public String encode(Object value) throws IOException {
    return writer.writeValueAsString(value);
  }

  /**
   * Converts JSON text to an object with Jackson.
   *
   * @param json the JSON text
   * @param type the class to create
   * @param <T> the class to create
   * @return the new object, or {@code null} for the JSON literal {@code null}
   * @throws IOException if the text is not valid JSON, has a property the class lacks (unless that
   *     check is turned off), or the class cannot be created
   */
  @Override
  public <T> T decode(String json, Class<T> type) throws IOException {
    return mapper.readValue(json, type);
  }
}
