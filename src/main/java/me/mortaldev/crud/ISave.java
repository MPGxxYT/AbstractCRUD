package me.mortaldev.crud;

public interface ISave {
  <T extends AbstractCRUD.Identifiable> void save(T object, String path);
}
