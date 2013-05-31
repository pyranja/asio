package at.ac.univie.isc.asio.common;

/**
 * SLF4J MDC context keys.
 * 
 * @author Chris Borckholder
 */
public final class LogContext {

	/** {@link #KEY_OP} value indicating a not yet parsed operation */
	public static final String NULL_OPERATION = "unknown";

	// MDC keys
	private static final String PREFIX = "asio.request.";

	/** full, absolute request URI */
	public static final String KEY_URI = PREFIX + "uri";
	/** request URI relative to web app base path (no params) */
	public static final String KEY_PATH = PREFIX + "path";
	/** request query parameters */
	public static final String KEY_PARAMS = PREFIX + "params";
	/** HTTP method of request */
	public static final String KEY_METHOD = PREFIX + "method";

	/** the dataset operation parsed from the request or <code>none</code> */
	public static final String KEY_OP = PREFIX + "operation";

	private LogContext() { /* constants class */}
}
