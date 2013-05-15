package at.ac.univie.isc.asio.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RandomIdGeneratorTest {

	private final IdGenerator subject = new RandomIdGenerator("test");

	@Test
	public void consecutive_ids_differ() throws Exception {
		final String first = subject.next();
		final String second = subject.next();
		assertNotEquals(first, second);
	}

	@Test
	public void ids_start_with_set_prefix() throws Exception {
		final String generated = subject.next();
		assertTrue(generated.startsWith("test-"));
	}

	@Test
	public void ids_have_a_non_empty_suffix() throws Exception {
		final String generated = subject.next();
		final String[] parts = generated.split("-", 2);
		assertFalse(parts[1].isEmpty());
	}
}
