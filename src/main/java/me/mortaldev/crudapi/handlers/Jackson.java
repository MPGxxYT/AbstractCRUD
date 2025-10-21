package me.mortaldev.crudapi.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.interfaces.Delete;
import me.mortaldev.crudapi.interfaces.Get;
import me.mortaldev.crudapi.interfaces.Handler;
import me.mortaldev.crudapi.interfaces.Save;
import me.mortaldev.crudapi.operations.delete.NormalDelete;
import me.mortaldev.crudapi.operations.get.JacksonGet;
import me.mortaldev.crudapi.operations.save.JacksonSave;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Jackson implements Handler {

  private static class Singleton {
    private static final Jackson INSTANCE = new Jackson();
  }

  public static Jackson getInstance() {
    return Singleton.INSTANCE;
  }

  private Jackson() {}

  private static final Logger LOGGER = Logger.getLogger("Jackson");

  public ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  public <T> T getJsonObject(File file, Class<T> clazz, CRUDAdapters crudAdapters) {
    if (file == null || clazz == null || crudAdapters == null) {
      LOGGER.log(Level.WARNING, "Invalid parameters: file, class, or adapters is null");
      return null;
    }
    if (!file.exists() || !file.isFile()) {
      LOGGER.log(Level.WARNING, "File does not exist or is not a file: " + file.getPath());
      return null;
    }

    ObjectMapper objectMapper = getObjectMapper();
    crudAdapters.getModules().forEach(objectMapper::registerModule);

    try {
      return objectMapper.readValue(file, clazz);
    } catch (IOException | JsonSyntaxException e) {
      LOGGER.log(Level.SEVERE, "Failed to read JSON from file: " + file.getPath(), e);
      return null;
    }
  }

  /**
   * Saves a JSON object to file using atomic write operation.
   *
   * <p>This method writes to a temporary file first, then atomically moves it to the target
   * location. This prevents data corruption if the process is interrupted mid-write.
   *
   * @param file Target file to save to
   * @param object Object to serialize
   * @param crudAdapters Adapters for custom serialization
   */
  public void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters) {
    if (file == null || object == null || crudAdapters == null) {
      LOGGER.log(Level.WARNING, "Invalid parameters: file, object, or adapters is null");
      return;
    }

    ObjectMapper objectMapper = getObjectMapper();
    crudAdapters.getModules().forEach(objectMapper::registerModule);

    File tempFile = null;
    try {
      // Ensure parent directory exists
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        Files.createDirectories(parentDir.toPath());
      }

      // Write to temporary file first (atomic write pattern)
      tempFile = new File(file.getParent(), file.getName() + ".tmp");

      try (FileWriter writer = new FileWriter(tempFile)) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, object);
      }

      // Atomically move temp file to target (prevents partial writes)
      Files.move(
          tempFile.toPath(),
          file.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);

    } catch (IOException | JsonIOException e) {
      LOGGER.log(Level.SEVERE, "Failed to save JSON to file: " + file.getPath(), e);
      // Clean up temp file if it exists
      if (tempFile != null && tempFile.exists()) {
        try {
          Files.delete(tempFile.toPath());
        } catch (IOException cleanupEx) {
          LOGGER.log(Level.WARNING, "Failed to delete temporary file: " + tempFile.getPath(), cleanupEx);
        }
      }
    }
  }

  @Override
  public Get get() {
    return new JacksonGet();
  }

  @Override
  public Save save() {
    return new JacksonSave();
  }

  @Override
  public Delete delete() {
    return new NormalDelete();
  }
}
