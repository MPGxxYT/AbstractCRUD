package me.mortaldev.abstractcrud.crudapi.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link Codec} that uses Gson and writes pretty-printed JSON.
 *
 * <h2>What Gson saves</h2>
 *
 * <ul>
 *   <li>Every field, private ones included, except {@code static} and {@code transient} fields.
 *       Getters and setters are not used.
 *   <li>Fields whose value is {@code null} are left out of the file.
 *   <li>To read an object back, Gson uses a no-argument constructor (it may be private) or a
 *       record's constructor. Without either, Gson creates the object without running any
 *       constructor, so field defaults such as {@code int max = 8;} are skipped and read as {@code
 *       0} when the file lacks the field.
 *   <li>Properties in the file that the class does not have are ignored.
 * </ul>
 *
 * <h2>Settings and custom serializers</h2>
 *
 * <p>Configure the {@link GsonBuilder} in the constructor callback. Pretty printing is already
 * turned on, and the {@link Gson} instance is built once, when the codec is created:
 *
 * <pre>{@code
 * Codec codec = new GsonCodec(builder -> builder
 *     .registerTypeAdapter(Location.class, new LocationGsonAdapter())
 *     .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
 * }</pre>
 *
 * <p>Instances are safe to share between threads and between CRUDs.
 *
 * @see JacksonCodec
 * @see me.mortaldev.abstractcrud.typeadapters.OptionalTypeAdapter
 */
public final class GsonCodec implements Codec {
  private final Gson gson;

  /** Creates a codec with Gson's default settings plus pretty printing. */
  public GsonCodec() {
    this(builder -> {});
  }

  /**
   * Creates a codec and lets you configure its {@link GsonBuilder} first: register type adapters,
   * factories, or other options.
   *
   * @param configure called once, right away, with a builder that already has pretty printing
   *     turned on. {@link GsonBuilder#create()} is called after it returns, so later changes to the
   *     builder have no effect.
   * @throws NullPointerException if {@code configure} is {@code null}
   */
  public GsonCodec(Consumer<GsonBuilder> configure) {
    Objects.requireNonNull(configure, "configure");
    GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
    configure.accept(builder);
    this.gson = builder.create();
  }

  /**
   * Converts an object to pretty-printed JSON with Gson.
   *
   * @param value the object to convert
   * @return the JSON text
   * @throws IOException if Gson cannot serialize the object, for example a field type Gson cannot
   *     access without an adapter
   */
  @Override
  public String encode(Object value) throws IOException {
    try {
      return gson.toJson(value);
    } catch (JsonParseException | IllegalArgumentException e) {
      throw new IOException("Gson could not serialize " + value.getClass().getName(), e);
    }
  }

  /**
   * Converts JSON text to an object with Gson.
   *
   * @param json the JSON text
   * @param type the class to create
   * @param <T> the class to create
   * @return the new object, or {@code null} for the JSON literal {@code null} or empty text
   * @throws IOException if the text is not valid JSON or does not fit the class
   */
  @Override
  public <T> T decode(String json, Class<T> type) throws IOException {
    try {
      return gson.fromJson(json, type);
    } catch (JsonParseException | IllegalArgumentException e) {
      throw new IOException("Gson could not parse JSON as " + type.getName(), e);
    }
  }
}
