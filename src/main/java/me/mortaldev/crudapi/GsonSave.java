package me.mortaldev.crudapi;

import java.io.File;
import java.util.HashMap;

public class GsonSave implements Save {

  @Override
  public <T> void save(T object, String id, String path, HashMap<Class<?>, Object> typeAdapterHashMap) {
    File filePath = new File(path + id + ".json");
    GSON.saveJsonObject(filePath, object, typeAdapterHashMap);
  }
}
