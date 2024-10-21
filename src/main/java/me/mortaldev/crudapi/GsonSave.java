package me.mortaldev.crudapi;

import java.io.File;
import java.util.HashMap;

public class GsonSave implements Save {
  @Override
  public <T extends CRUD.Identifiable> void save(T object, String path, HashMap<Class<?>, Object> typeAdapterHashMap) {
    File filePath = new File(path + object.getID() + ".json");
    GSON.saveJsonObject(filePath, object, typeAdapterHashMap);
  }
}
