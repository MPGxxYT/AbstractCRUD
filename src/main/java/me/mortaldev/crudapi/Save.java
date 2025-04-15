package me.mortaldev.crudapi;

import java.util.HashMap;

public interface Save {
  <T> void save(T object, String id, String path, HashMap<Class<?>, Object> typeAdapterHashMap);
}
