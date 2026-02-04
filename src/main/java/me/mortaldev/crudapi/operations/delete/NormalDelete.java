package me.mortaldev.crudapi.operations.delete;

import me.mortaldev.crudapi.interfaces.Delete;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NormalDelete implements Delete {
  private static final String DATA_DELETED = "Data Deleted: {0}";
  private static final Logger LOGGER = Logger.getLogger("NormalDelete");

  @Override
  public boolean delete(String id, String path) {
    if (id == null || id.isEmpty()) {
      LOGGER.log(Level.WARNING, "Cannot delete: ID is null or empty");
      return false;
    }
    if (path == null) {
      LOGGER.log(Level.WARNING, "Cannot delete: path is null");
      return false;
    }
    File filePath = new File(path, id + ".json");
    if (!filePath.exists()) {
      LOGGER.log(Level.WARNING, "Could not delete data: ''{0}'' does not exist.", id);
      return false;
    }
    try {
      Files.delete(filePath.toPath());
      String message = MessageFormat.format(DATA_DELETED, id);
      LOGGER.log(Level.INFO, message);
      return true;
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to delete: " + id, e);
      return false;
    }
  }
}
