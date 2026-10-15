package me.mortaldev.crudapi.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link Codec} backed by {@link Gson}, writing pretty-printed JSON.
 *
 * <p>The {@link Gson} instance is built once. Register type adapters and factories in the
 * constructor callback:
 *
 * <pre>{@code
 * Codec codec = new GsonCodec(b -> b.registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
 * }</pre>
 */
public final class GsonCodec implements Codec {
  private final Gson gson;

  /** Creates a codec with default Gson settings plus pretty printing. */
  public GsonCodec() {
    this(builder -> {});
  }

  /**
   * Creates a codec whose builder is configured by {@code configure}. Pretty printing is already
   * enabled on the builder.
   *
   * @param configure receives the builder before {@link GsonBuilder#create()} is called
   */
  public GsonCodec(Consumer<GsonBuilder> configure) {
    Objects.requireNonNull(configure, "configure");
    GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
    configure.accept(builder);
    this.gson = builder.create();
  }

  @Override
  public String encode(Object value) throws IOException {
    try {
      return gson.toJson(value);
    } catch (JsonParseException | IllegalArgumentException e) {
      throw new IOException("Gson could not serialize " + value.getClass().getName(), e);
    }
  }

  @Override
  public <T> T decode(String json, Class<T> type) throws IOException {
    try {
      return gson.fromJson(json, type);
    } catch (JsonParseException | IllegalArgumentException e) {
      throw new IOException("Gson could not parse JSON as " + type.getName(), e);
    }
  }
}
