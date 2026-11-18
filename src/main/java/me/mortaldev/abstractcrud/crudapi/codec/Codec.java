package me.mortaldev.abstractcrud.crudapi.codec;

import java.io.IOException;
import me.mortaldev.abstractcrud.crudapi.CRUD;

/**
 * Converts objects to JSON text and back. A {@link CRUD} uses it for every read and write; the
 * codec decides what the JSON looks like, and the CRUD handles the files.
 *
 * <p>Two implementations are included: {@link JacksonCodec} and {@link GsonCodec}. Create one in
 * {@code onEnable} and pass the same instance to every CRUD that should use the same settings and
 * custom serializers.
 *
 * <p>You can also call a codec directly, without files, to check that a class converts the way
 * you expect:
 *
 * <pre>{@code
 * String json = codec.encode(settings);
 * Settings back = codec.decode(json, Settings.class);
 * }</pre>
 *
 * <p>Implementations must be safe to use from several threads at once, because one codec is
 * shared by several CRUDs.
 *
 * @see JacksonCodec
 * @see GsonCodec
 */
public interface Codec {

  /**
   * Converts an object to JSON text.
   *
   * @param value the object to convert
   * @return the JSON text
   * @throws IOException if the object cannot be converted, for example because a field's type has
   *     no serializer
   */
  String encode(Object value) throws IOException;

  /**
   * Converts JSON text to an object of the given class.
   *
   * @param json the JSON text
   * @param type the class to create, for example {@code Settings.class}
   * @param <T> the class to create
   * @return the new object, or {@code null} if the text is the JSON literal {@code null}
   * @throws IOException if the text is not valid JSON, or does not fit the class
   */
  <T> T decode(String json, Class<T> type) throws IOException;
}
