package at.ac.univie.isc.asio.common;

/**
 * A marker interface for object which allocate resources (e.g. some persistent
 * storage like files or database entries) that may/should be freed, if the
 * object is not needed anymore.
 * <p>
 * Similar to {@link AutoCloseable}, but the marked object is intended to be
 * kept for reuse (e.g. reading a file multiple times).
 * </p>
 * 
 * @author Chris Borckholder
 */
public interface Disposable {

	/**
	 * Permanently free the resources allocated by this object. Afterwards this
	 * object may not be usable anymore.
	 * 
	 * @throws Exception
	 *             on any error. Subclasses should throw implementation specific
	 *             exceptions.
	 */
	void dispose() throws Exception;
}
