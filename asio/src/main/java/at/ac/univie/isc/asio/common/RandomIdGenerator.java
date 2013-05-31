package at.ac.univie.isc.asio.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import java.util.Random;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;

/**
 * Create pseudo-randomly identifier that should probably be unique in this JVM
 * process.
 * 
 * @author Chris Borckholder
 */
public final class RandomIdGenerator implements IdGenerator {

	public static IdGenerator withPrefix(final String prefix) {
		checkNotNull(emptyToNull(prefix));
		return new RandomIdGenerator(prefix);
	}

	private final String prefix;
	private final Random rng;

	@VisibleForTesting
	RandomIdGenerator(final String prefix) {
		super();
		this.prefix = prefix;
		rng = new Random();
	}

	/**
	 * @return a new identifier in this format "{prefix}:{random UUID}"
	 */
	@Override
	public String next() {
		final long lsb = rng.nextLong();
		final long msb = System.currentTimeMillis();
		final UUID id = new UUID(msb, lsb);
		return prefix + ":" + id.toString();
	}
}
