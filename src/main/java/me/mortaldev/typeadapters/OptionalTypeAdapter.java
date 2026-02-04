package me.mortaldev.typeadapters;

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
 * A GSON TypeAdapterFactory for handling {@link Optional} types.
 *
 * <p>This factory creates type adapters that properly serialize and deserialize
 * Optional values while preserving the inner type information.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
 *     .create();
 * }</pre>
 *
 * <p><b>Serialization format:</b>
 * <ul>
 *   <li>Empty optional: {@code null}</li>
 *   <li>Present optional: the contained value directly</li>
 * </ul>
 */
public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {

  /**
   * A factory for creating OptionalTypeAdapter instances.
   * Register this with GsonBuilder to enable Optional support.
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

  @Override
  public void write(JsonWriter out, Optional<T> value) throws IOException {
    if (value == null || value.isEmpty()) {
      out.nullValue();
    } else {
      innerAdapter.write(out, value.get());
    }
  }

  @Override
  public Optional<T> read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return Optional.empty();
    }
    return Optional.of(innerAdapter.read(in));
  }
}
