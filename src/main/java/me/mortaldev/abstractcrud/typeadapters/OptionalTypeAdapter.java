package me.mortaldev.abstractcrud.typeadapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Lets Gson save and read {@link Optional} fields. Gson cannot handle {@code Optional} on its own.
 *
 * <p>Register {@link #FACTORY} with the {@code GsonCodec}; it then applies to every {@code
 * Optional} field, whatever its value type:
 *
 * <pre>{@code
 * Codec codec = new GsonCodec(builder -> builder.registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
 * }</pre>
 *
 * <p>In the JSON, a present value is written as the value itself, and an empty {@code Optional}
 * as {@code null}. Gson leaves {@code null} fields out of the file by default, so an empty
 * optional field is simply missing. When such a file is read, the field keeps the value it was
 * given when the object was created. Initialize optional fields so they are never {@code null}:
 *
 * <pre>{@code
 * private Optional<String> nickname = Optional.empty();
 * }</pre>
 *
 * <p>This class is for Gson only. With Jackson, add the {@code jackson-datatype-jdk8} dependency
 * and register its {@code Jdk8Module} instead.
 *
 * @param <T> the type inside the {@code Optional}
 */
public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {

  /**
   * The factory to register with Gson. It creates an adapter for each {@code Optional<X>} type it
   * meets, using Gson's own adapter for {@code X}, and ignores every other type.
   */
  public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
    @Override
    @SuppressWarnings("unchecked")
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
      Class<? super R> rawType = type.getRawType();
      if (rawType != Optional.class) {
        return null;
      }
      Type innerType = getInnerType(type.getType());
      TypeAdapter<?> innerAdapter = gson.getAdapter(TypeToken.get(innerType));
      return (TypeAdapter<R>) new OptionalTypeAdapter<>(innerAdapter);
    }

    private Type getInnerType(Type type) {
      if (type instanceof ParameterizedType parameterized) {
        Type[] typeArguments = parameterized.getActualTypeArguments();
        if (typeArguments.length > 0) {
          return typeArguments[0];
        }
      }
      return Object.class;
    }
  };

  private final TypeAdapter<T> innerAdapter;

  private OptionalTypeAdapter(TypeAdapter<T> innerAdapter) {
    this.innerAdapter = innerAdapter;
  }

  /**
   * Writes the value inside the optional, or {@code null} if it is empty. Called by Gson.
   *
   * @param out where Gson is writing
   * @param value the optional to write; {@code null} is written like an empty optional
   * @throws IOException if writing fails
   */
  @Override
  public void write(JsonWriter out, Optional<T> value) throws IOException {
    if (value == null || value.isEmpty()) {
      out.nullValue();
    } else {
      innerAdapter.write(out, value.get());
    }
  }

  /**
   * Reads a value into an optional: {@code null} becomes {@link Optional#empty()}. Called by Gson.
   *
   * @param in where Gson is reading
   * @return the value wrapped in an optional, never {@code null}
   * @throws IOException if reading fails
   */
  @Override
  public Optional<T> read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return Optional.empty();
    }
    return Optional.of(innerAdapter.read(in));
  }
}
