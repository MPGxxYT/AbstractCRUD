package me.mortaldev.abstractcrud.crudapi.loading;

/**
 * Something that reads its data from disk when told to. {@code CRUDManager} and {@code SingleCRUD}
 * implement it, and {@link CRUDRegistry#initialize()} calls it on every registered object.
 *
 * <p>Implement it in your own classes to have them loaded by the same {@link CRUDRegistry}.
 */
public interface ILoadable {

  /**
   * Reads this object's data from disk, replacing what is in memory. It may be called more than
   * once, for example by a reload command.
   */
  void load();
}
