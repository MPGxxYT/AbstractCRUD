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
import java.util.logging.Level;
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

  public void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters) {
    ObjectMapper objectMapper = getObjectMapper();
    crudAdapters.getModules().forEach(objectMapper::registerModule);
    try {
      file.getParentFile().mkdirs();
      file.createNewFile();
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(file), object);
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
