package me.mortaldev.crudapi.operations.get;

import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.GSON;
import me.mortaldev.crudapi.interfaces.Get;

import java.io.File;
import java.util.Optional;

public class GsonGet implements Get {
  private final GSON gson;

  public GsonGet(GSON gson) {
    this.gson = gson;
  }

  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz, CRUDAdapters crudAdapters) {
    if (id == null || id.isEmpty() || path == null || clazz == null) {
      return Optional.empty();
    }
    File filePath = new File(path, id + ".json");
    if (filePath.exists() && filePath.isFile()) {
      return Optional.ofNullable(gson.getJsonObject(filePath, clazz, crudAdapters));
    }
    return Optional.empty();
  }
}
