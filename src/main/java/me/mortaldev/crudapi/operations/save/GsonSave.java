package me.mortaldev.crudapi.operations.save;

import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.GSON;
import me.mortaldev.crudapi.interfaces.Save;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GsonSave implements Save {
  private static final Logger LOGGER = Logger.getLogger("GsonSave");
  private final GSON gson;

  public GsonSave(GSON gson) {
    this.gson = gson;
  }

  @Override
  public <T> void save(T object, String id, String path, CRUDAdapters crudAdapters) {
    if (object == null || id == null || id.isEmpty() || path == null) {
      LOGGER.log(Level.WARNING, "Cannot save: object, id, or path is null/empty");
      return;
    }
    File filePath = new File(path, id + ".json");
    gson.saveJsonObject(filePath, object, crudAdapters);
  }
}
