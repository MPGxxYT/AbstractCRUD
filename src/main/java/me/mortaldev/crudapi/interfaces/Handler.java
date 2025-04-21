package me.mortaldev.crudapi.interfaces;

import me.mortaldev.crudapi.CRUDAdapters;

import java.io.File;

public interface Handler {
  <T> T getJsonObject(File file, Class<T> clazz, CRUDAdapters crudAdapters);

  void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters);

  Get get();
  Save save();
  Delete delete();
}
