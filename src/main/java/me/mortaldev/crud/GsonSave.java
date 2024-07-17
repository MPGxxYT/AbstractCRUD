package me.mortaldev.crud;

import java.io.File;

public class GsonSave implements ISave {
  @Override
  public <T extends AbstractCRUD.Identifiable> void save(T object, String path) {
    File filePath = new File(path + object.getID() + ".json");
    GSON.saveJsonObject(filePath, object);
  }
}
