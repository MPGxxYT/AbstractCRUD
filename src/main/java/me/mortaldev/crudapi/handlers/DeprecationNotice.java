package me.mortaldev.crudapi.handlers;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Logs a one-time console warning that the plugin calling into this library uses the 1.x API.
 *
 * <p>The flag is static, so the warning fires once per class loader: each plugin that shades its
 * own copy of the library warns once.
 */
final class DeprecationNotice {
  private static final Logger LOGGER = Logger.getLogger("AbstractCRUD");
  private static final AtomicBoolean WARNED = new AtomicBoolean(false);

  /** Root package of this library, correct even when a plugin relocates it while shading. */
  private static final String LIBRARY_ROOT =
      DeprecationNotice.class.getPackageName().replaceFirst("\\.handlers$", "");

  private DeprecationNotice() {}

  static void warnOnce() {
    if (!WARNED.compareAndSet(false, true)) {
      return;
    }
    String caller = callerPackage().orElse("an unknown plugin");
    LOGGER.warning(
        "[AbstractCRUD] Plugin code in '"
            + caller
            + "' uses the AbstractCRUD 1.x API, which is removed in 2.0.0. Plugin developers:"
            + " see https://github.com/MPGxxYT/AbstractCRUD for the new usage.");
  }

  private static Optional<String> callerPackage() {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(
            frames ->
                frames
                    .map(frame -> frame.getDeclaringClass().getPackageName())
                    .filter(pkg -> !pkg.equals(LIBRARY_ROOT) && !pkg.startsWith(LIBRARY_ROOT + "."))
                    .findFirst());
  }
}
