package me.mortaldev.crud;

public interface IDelete {
  <T extends AbstractCRUD.Identifiable> void delete(T object, String path);
}
