package me.mortaldev.crudapi.operations.save;

import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.GSON;
import me.mortaldev.crudapi.interfaces.Save;

import java.io.File;

public class GsonSave implements Save {

  @Override
  public <T> void save(T object, String id, String path, CRUDAdapters crudAdapters) {
    File filePath = new File(path + id + ".json");
    GSON.getInstance().saveJsonObject(filePath, object, crudAdapters);
  }
}
