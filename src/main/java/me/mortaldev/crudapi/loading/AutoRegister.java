package me.mortaldev.crudapi.loading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a CRUD manager class for automatic discovery and registration by the CRUDRegistry during
 * plugin startup.
 *
 * @deprecated Removed in 2.0.0. See https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegister {}
