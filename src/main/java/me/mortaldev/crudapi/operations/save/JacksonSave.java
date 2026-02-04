package me.mortaldev.crudapi.operations.save;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.handlers.Jackson;
import me.mortaldev.crudapi.interfaces.Save;

public class JacksonSave implements Save {
  private static final Logger LOGGER = Logger.getLogger("JacksonSave");
  private final Jackson jackson;

  public JacksonSave(Jackson jackson) {
    this.jackson = jackson;
  }

  @Override
  public <T> void save(T object, String id, String path, CRUDAdapters crudAdapters) {
    if (object == null || id == null || id.isEmpty() || path == null) {
      LOGGER.log(Level.WARNING, "Cannot save: object, id, or path is null/empty");
      return;
    }
    File filePath = new File(path, id + ".json");
    jackson.saveJsonObject(filePath, object, crudAdapters);
  }
}
