package me.mortaldev.crudapi.interfaces;

import me.mortaldev.crudapi.CRUDAdapters;

import java.io.File;

/**
 * @deprecated Removed in 2.0.0. See https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public interface Handler {
  <T> T getJsonObject(File file, Class<T> clazz, CRUDAdapters crudAdapters);

  void saveJsonObject(File file, Object object, CRUDAdapters crudAdapters);

  Get get();
  Save save();
  Delete delete();
}
