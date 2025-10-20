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
    ObjectMapper objectMapper = getObjectMapper();
    crudAdapters.getModules().forEach(objectMapper::registerModule);
    if (!file.exists()) {
      LOGGER.log(Level.WARNING, "File does not exist: " + file.getPath());
      return null;
    }

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
    ObjectMapper objectMapper = getObjectMapper();
    crudAdapters.getModules().forEach(objectMapper::registerModule);

    try {
      // Ensure parent directory exists
      Path parentDir = file.getParentFile().toPath();
      Files.createDirectories(parentDir);

      // Write to temporary file first (atomic write pattern)
      File tempFile = new File(file.getParent(), file.getName() + ".tmp");

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
