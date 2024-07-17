package me.mortaldev.crud;

public interface IDelete {
  <T extends AbstractCRUD.Identifiable> boolean delete(T object, String path);
}
