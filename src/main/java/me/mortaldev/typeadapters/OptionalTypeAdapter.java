package me.mortaldev.typeadapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {

  @Override
  public void write(JsonWriter out, Optional<T> value) throws IOException {
    if (value == null || value.isEmpty()) {
      out.nullValue();
      return;
    }
    out.beginObject();
    out.name("present").value(true);
    out.name("value");
    out.jsonValue(new Gson().toJson(value.get()));
    out.endObject();
  }

  @Override
  public Optional<T> read(JsonReader in) throws IOException {
    in.beginObject();
    boolean isPresent = false;
    T value = null;
    while (in.hasNext()) {
      String name = in.nextName();
      if (name.equals("present")) {
        isPresent = in.nextBoolean();
      } else if (name.equals("value") && isPresent) {
        try {
          value = new Gson().fromJson(in.nextString(), new TypeToken<T>() {}.getType());
        } catch (Exception e) {
          Logger.getLogger("AbstractCRUD")
              .severe("Error deserializing Optional value: " + e.getMessage());
          return Optional.empty();
        }
      }
    }
    in.endObject();
    return isPresent ? Optional.of(value) : Optional.empty();
  }
}
