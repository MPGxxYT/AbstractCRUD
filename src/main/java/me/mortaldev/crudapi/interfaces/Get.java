package me.mortaldev.crudapi.interfaces;

import me.mortaldev.crudapi.CRUDAdapters;

import java.util.Optional;

/**
 * @deprecated Removed in 2.0.0. See https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public interface Get {
  <T> Optional<T> get(String id, String path, Class<T> clazz, CRUDAdapters crudAdapters);
}
