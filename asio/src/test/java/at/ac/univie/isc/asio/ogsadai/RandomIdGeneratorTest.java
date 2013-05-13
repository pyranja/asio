package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class RandomIdGeneratorTest {

	private final IdGenerator subject = new RandomIdGenerator("test");

	@Test
	public void conscutive_ids_differ() throws Exception {
		final String first = subject.next();
		final String second = subject.next();
		assertNotEquals(first, second);
	}
}
