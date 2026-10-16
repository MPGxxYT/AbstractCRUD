package me.mortaldev.abstractcrud.crudapi.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link Codec} backed by a Jackson {@link ObjectMapper}, writing pretty-printed JSON.
 *
 * <p>The mapper is built once. Register modules, serializers and features in the constructor
 * callback:
 *
 * <pre>{@code
 * Codec codec = new JacksonCodec(mapper -> mapper.registerModule(new JavaTimeModule()));
 * }</pre>
 */
public final class JacksonCodec implements Codec {
  private final ObjectMapper mapper;
  private final ObjectWriter writer;

  /** Creates a codec with a default {@link ObjectMapper}. */
  public JacksonCodec() {
    this(mapper -> {});
  }

  /**
   * Creates a codec whose mapper is configured by {@code configure} before first use.
   *
   * @param configure receives the new mapper; must not keep a reference to it
   */
  public JacksonCodec(Consumer<ObjectMapper> configure) {
    Objects.requireNonNull(configure, "configure");
    this.mapper = new ObjectMapper();
    configure.accept(mapper);
    this.writer = mapper.writerWithDefaultPrettyPrinter();
  }

  @Override
  public String encode(Object value) throws IOException {
    return writer.writeValueAsString(value);
  }

  @Override
  public <T> T decode(String json, Class<T> type) throws IOException {
    return mapper.readValue(json, type);
  }
}
