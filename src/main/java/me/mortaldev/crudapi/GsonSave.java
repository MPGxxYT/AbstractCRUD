package me.mortaldev.crudapi;

import java.io.File;

public class GsonSave implements Save {
  @Override
  public <T extends CRUD.Identifiable> void save(T object, String path) {
    File filePath = new File(path + object.getID() + ".json");
    GSON.saveJsonObject(filePath, object);
  }
}
