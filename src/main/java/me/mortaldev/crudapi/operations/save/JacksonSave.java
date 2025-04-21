package me.mortaldev.crudapi.operations.save;

import java.io.File;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.Jackson;
import me.mortaldev.crudapi.interfaces.Save;

public class JacksonSave implements Save {

  @Override
  public <T> void save(T object, String id, String path, CRUDAdapters crudAdapters) {
    File filePath = new File(path + id + ".json");
    Jackson.getInstance().saveJsonObject(filePath, object, crudAdapters);
  }
}
