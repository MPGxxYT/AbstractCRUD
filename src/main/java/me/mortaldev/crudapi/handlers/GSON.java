package me.mortaldev.crudapi.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import me.mortaldev.crudapi.CRUDAdapters;
import me.mortaldev.crudapi.interfaces.Delete;
import me.mortaldev.crudapi.interfaces.Get;
import me.mortaldev.crudapi.interfaces.Handler;
import me.mortaldev.crudapi.interfaces.Save;
import me.mortaldev.crudapi.operations.delete.NormalDelete;
import me.mortaldev.crudapi.operations.get.GsonGet;
import me.mortaldev.crudapi.operations.save.GsonSave;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GSON implements Handler {

  private static class Singleton {
    private static final GSON INSTANCE = new GSON();
  }

  public static synchronized GSON getInstance() {
    return Singleton.INSTANCE;
  }

  private GSON() {}

  private static final Logger LOGGER = Logger.getLogger("GSON");

  @Override
  public <T> T getJsonObject(File file, Class<T> clazz, CRUDAdapters crudAdapters) {
    GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
    Map<Class<?>, Object> typeAdapters = crudAdapters.getTypeAdapters();
    if (!typeAdapters.isEmpty()) {
      typeAdapters.forEach(gsonBuilder::registerTypeAdapter);
    }
    Gson gson = gsonBuilder.create();
    if (!file.exists()) {
      LOGGER.log(Level.WARNING, "File does not exist: " + file.getPath());
      return null;
    }

    try (Reader reader = new FileReader(file)) {
      return gson.fromJson(reader, clazz);
    } catch (IOException | JsonSyntaxException e) {
      LOGGER.log(Level.SEVERE, "Failed to read JSON from file: " + file.getPath(), e);
      return null;
    }
  }

  @Override
  public void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters) {
    GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
    Map<Class<?>, Object> typeAdapters = crudAdapters.getTypeAdapters();
    if (!typeAdapters.isEmpty()) {
      typeAdapters.forEach(gsonBuilder::registerTypeAdapter);
    }
    Gson gson = gsonBuilder.create();
    try {
      file.getParentFile().mkdirs();
      file.createNewFile();
      try (Writer writer = new FileWriter(file, false)) {
        gson.toJson(object, writer);
        writer.flush();
      }
    } catch (IOException | JsonIOException e) {
      LOGGER.log(Level.SEVERE, "Failed to save JSON to file: " + file.getPath(), e);
    }
  }

  @Override
  public Get get() {
    return new GsonGet();
  }

  @Override
  public Save save() {
    return new GsonSave();
  }

  @Override
  public Delete delete() {
    return new NormalDelete();
  }
}
