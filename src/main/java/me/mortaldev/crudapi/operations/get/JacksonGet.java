package me.mortaldev.crudapi.operations.get;

import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.Jackson;
import me.mortaldev.crudapi.interfaces.Get;

import java.io.File;
import java.util.Optional;

public class JacksonGet implements Get {

  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz, CRUDAdapters crudAdapters) {
    File filePath = new File(path + id + ".json");
    if (filePath.exists()) {
      return Optional.ofNullable(Jackson.getInstance().getJsonObject(filePath, clazz, crudAdapters));
    } else {
      return Optional.empty();
    }
  }
}
