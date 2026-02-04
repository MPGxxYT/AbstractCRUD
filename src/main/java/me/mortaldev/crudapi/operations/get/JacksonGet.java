package me.mortaldev.crudapi.operations.get;

import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.Jackson;
import me.mortaldev.crudapi.interfaces.Get;

import java.io.File;
import java.util.Optional;

public class JacksonGet implements Get {
  private final Jackson jackson;

  public JacksonGet(Jackson jackson) {
    this.jackson = jackson;
  }

  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz, CRUDAdapters crudAdapters) {
    if (id == null || id.isEmpty() || path == null || clazz == null) {
      return Optional.empty();
    }
    File filePath = new File(path, id + ".json");
    if (filePath.exists() && filePath.isFile()) {
      return Optional.ofNullable(jackson.getJsonObject(filePath, clazz, crudAdapters));
    }
    return Optional.empty();
  }
}
