package me.mortaldev.crudapi;

public interface IDelete {
  <T extends AbstractCRUD.Identifiable> boolean delete(T object, String path);
}
