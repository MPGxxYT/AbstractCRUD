package me.mortaldev.crudapi;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;

public class GsonGet implements Get {
  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz, HashMap<Class<?>, Object> typeAdapterHashMap) {
    File filePath = new File(path + id + ".json");
    if (filePath.exists()) {
      return Optional.ofNullable(GSON.getJsonObject(filePath, clazz, typeAdapterHashMap));
    } else {
      return Optional.empty();
    }
  }
}
