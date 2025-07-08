package me.mortaldev.crudapi.loading;

/**
 * Marks a class as a singleton that can be automatically discovered and
 * registered by the CRUDRegistry.
 * <p>
 * A class implementing this interface is expected to:
 * <ol>
 *   <li>Be annotated with {@link AutoRegister}.</li>
 *   <li>Implement the singleton pattern with a public static {@code getInstance()} method.</li>
 * </ol>
 */
public interface IRegistrable {
  // This is a marker interface, so it has no methods.
  // Its purpose is to enforce a design contract.
}