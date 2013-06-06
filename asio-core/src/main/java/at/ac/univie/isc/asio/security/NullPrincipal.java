package at.ac.univie.isc.asio.security;

import java.security.Principal;

/**
 * Indicate an unavailable {@link Principal}.
 * 
 * @author Chris Borckholder
 */
public final class NullPrincipal implements Principal {

	public static final Principal INSTANCE = new NullPrincipal();

	private NullPrincipal() {}

	@Override
	public String getName() {
		return "null";
	}

	@Override
	public String toString() {
		return "[NullPrincipal]";
	}
}
