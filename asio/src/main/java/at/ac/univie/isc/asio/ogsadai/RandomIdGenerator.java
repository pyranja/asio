package at.ac.univie.isc.asio.ogsadai;

import java.util.Random;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;

/**
 * Create pseudo-randomly identifier that should probably be unique in this JVM
 * process.
 * 
 * @author Chris Borckholder
 */
public class RandomIdGenerator implements IdGenerator {

	private final String prefix;
	private final Random rng;

	RandomIdGenerator(final String prefix) {
		super();
		this.prefix = prefix;
		rng = new Random();
	}

	@VisibleForTesting
	RandomIdGenerator(final Random rng) {
		prefix = "test";
		this.rng = rng;
	}

	/**
	 * @return a new identifier in this format "{prefix}-{random UUID}"
	 */
	@Override
	public String next() {
		final long lsb = rng.nextLong();
		final long msb = System.currentTimeMillis();
		final UUID id = new UUID(msb, lsb);
		return prefix + "-" + id.toString();
	}
}
