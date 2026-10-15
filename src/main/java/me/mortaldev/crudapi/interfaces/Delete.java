package me.mortaldev.crudapi.interfaces;

/**
 * @deprecated Removed in 2.0.0. See https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public interface Delete {
  boolean delete(String id, String path);
}
