package me.mortaldev.crudapi;

public interface ISave {
  <T extends AbstractCRUD.Identifiable> void save(T object, String path);
}
