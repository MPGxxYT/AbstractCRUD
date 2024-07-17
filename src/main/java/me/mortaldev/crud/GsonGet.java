package me.mortaldev.crud;

import java.io.File;
import java.util.Optional;

public class GsonGet implements IGet {
  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz) {
    File filePath = new File(path + id + ".json");
    if (filePath.exists()) {
      return Optional.ofNullable(GSON.getJsonObject(filePath, clazz));
    } else {
      return Optional.empty();
    }
  }
}
