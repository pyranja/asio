package at.ac.univie.isc.asio.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for resource clean up.
 * 
 * @author Chris Borckholder
 */
public final class Resources {

	private static final String ERROR_MSG = "!! error while cleaning up {} : {}";

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(Resources.class);

	/**
	 * Close the given {@link AutoCloseable resource} if it is not null. If an
	 * exception occurs while closing, it is logged with level WARN, but not
	 * rethrown.
	 * 
	 * @param that
	 *            to be closed
	 */
	public static void close(final AutoCloseable that) {
		if (that != null) {
			try {
				that.close();
			} catch (final Exception e) {
				log.warn(ERROR_MSG, that, e.getMessage(), e);
			}
		} else {
			log.warn(ERROR_MSG, that, "was null");
		}
	}

	/**
	 * Disposes the given {@link Disposable resource} if it is not null. If an
	 * exception occurs while disposing, it is logged with level WARN, but not
	 * rethrown.
	 * 
	 * @param that
	 *            to be closed
	 */
	public static void dispose(final Disposable that) {
		if (that != null) {
			try {
				that.dispose();
			} catch (final Exception e) {
				log.warn(ERROR_MSG, that, e.getMessage(), e);
			}
		} else {
			log.warn(ERROR_MSG, that, "was null");
		}
	}

	private Resources() {/* static helper */}
}
