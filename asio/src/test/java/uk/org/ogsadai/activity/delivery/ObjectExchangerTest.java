package uk.org.ogsadai.activity.delivery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.delivery.ObjectExchanger.IdTakenException;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ObjectExchangerTest {

	private ObjectExchanger<Long> subject;
	private Long thing;

	@Before
	public void setUp() {
		thing = new Long(1L);
		subject = new ObjectExchanger<>();
	}

	@Test(expected = NullPointerException.class)
	public void cannot_offer_null_id() {
		subject.offer(null, thing);
	}

	@Test(expected = NullPointerException.class)
	public void cannot_offer_null_stream() {
		subject.offer("valid", null);
	}

	@Test(expected = NullPointerException.class)
	public void cannot_take_null_id() {
		subject.take(null);
	}

	@Test(expected = IdTakenException.class)
	public void storing_id_twice_fails() {
		subject.offer("test", thing);
		subject.offer("test", thing);
	}

	@Test
	public void can_take_offered_stream() {
		subject.offer("test", thing);
		final Optional<Long> taken = subject.take("test");
		assertTrue(taken.isPresent());
		assertSame(thing, taken.get());
	}

	@Test
	public void taking_not_mapped_yields_absent_optional() {
		final Optional<Long> taken = subject.take("not-there");
		assertFalse(taken.isPresent());
	}

	@Test
	public void stream_is_absent_after_take() {
		subject.offer("test", thing);
		subject.take("test");
		final Optional<Long> taken = subject.take("test");
		assertFalse(taken.isPresent());
	}

	@Test
	public void can_reuse_id_after_take() {
		subject.offer("test", thing);
		subject.take("test");
		subject.offer("test", thing);
	}
}
