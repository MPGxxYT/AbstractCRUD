package me.mortaldev.abstractcrud.crudapi.codec;

import me.mortaldev.abstractcrud.crudapi.CRUD;

import java.io.IOException;

/**
 * Converts objects to and from JSON text. {@link CRUD} owns the files; a
 * codec only decides how an object looks as JSON.
 *
 * <p>Implementations must be safe to share between threads and between CRUD instances. Sharing one
 * codec across several CRUDs is how serialization settings are applied globally.
 */
public interface Codec {

  /**
   * Serializes a value to JSON text.
   *
   * @throws IOException if the value cannot be serialized
   */
  String encode(Object value) throws IOException;

  /**
   * Parses JSON text into an instance of {@code type}.
   *
   * @return the parsed value, or {@code null} if the text is the JSON literal {@code null}
   * @throws IOException if the text is not valid JSON for {@code type}
   */
  <T> T decode(String json, Class<T> type) throws IOException;
}
