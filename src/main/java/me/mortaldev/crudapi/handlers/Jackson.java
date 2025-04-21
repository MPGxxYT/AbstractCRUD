package me.mortaldev.crudapi.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.interfaces.Delete;
import me.mortaldev.crudapi.interfaces.Get;
import me.mortaldev.crudapi.interfaces.Handler;
import me.mortaldev.crudapi.interfaces.Save;
import me.mortaldev.crudapi.operations.delete.NormalDelete;
import me.mortaldev.crudapi.operations.get.JacksonGet;
import me.mortaldev.crudapi.operations.save.JacksonSave;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class Jackson implements Handler {

  private static class Singleton {
    private static final Jackson INSTANCE = new Jackson();
  }

  public static synchronized Jackson getInstance() {
    return Singleton.INSTANCE;
  }

  private Jackson() {}

  private static final Logger LOGGER = Logger.getLogger("Jackson");

  public <T> T getJsonObject(File file, Class<T> clazz, CRUDAdapters crudAdapters) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(crudAdapters.getModule());
    try {
      return objectMapper.readValue(file, clazz);
    } catch (IOException e) {
      LOGGER.severe("Failed to read JSON from file: " + file.getPath());
      return null;
    }
  }

  public void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(crudAdapters.getModule());
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(file), object);
    } catch (IOException e) {
      LOGGER.severe("Failed to save JSON to file: " + file.getPath());
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
