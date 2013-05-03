package uk.org.ogsadai.activity.delivery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.delivery.StreamExchanger.IdTakenException;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class StreamExchangerTest {

	private StreamExchanger subject;
	@Mock private OutputStream stream;

	@Before
	public void setUp() {
		subject = new StreamExchanger();
	}

	@Test(expected = NullPointerException.class)
	public void cannot_offer_null_id() {
		subject.offer(null, stream);
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
		subject.offer("test", stream);
		subject.offer("test", stream);
	}

	@Test
	public void can_take_offered_stream() {
		subject.offer("test", stream);
		final Optional<OutputStream> taken = subject.take("test");
		assertTrue(taken.isPresent());
		assertSame(stream, taken.get());
	}

	@Test
	public void taking_not_mapped_yields_absent_optional() {
		final Optional<OutputStream> taken = subject.take("not-there");
		assertFalse(taken.isPresent());
	}

	@Test
	public void stream_is_absent_after_take() {
		subject.offer("test", stream);
		subject.take("test");
		final Optional<OutputStream> taken = subject.take("test");
		assertFalse(taken.isPresent());
	}

	@Test
	public void can_reuse_id_after_take() {
		subject.offer("test", stream);
		subject.take("test");
		subject.offer("test", stream);
	}
}
