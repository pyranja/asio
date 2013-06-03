package at.ac.univie.isc.asio.transport.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DynamicLimitChannelTest {

	private DynamicLimitChannelDecorator subject;
	@Mock private ReadableByteChannel channel;
	@Mock private BufferSync sync;

	@Before
	public void setUp() {
		Mockito.when(channel.isOpen()).thenReturn(true);
		subject = new DynamicLimitChannelDecorator(channel, sync);
	}

	// constructor invariances

	@Test(expected = NullPointerException.class)
	public void null_channel_fails() {
		subject = new DynamicLimitChannelDecorator(null, sync);
	}

	@Test(expected = NullPointerException.class)
	public void null_sync_fails() {
		subject = new DynamicLimitChannelDecorator(channel, null);
	}

	@Test
	public void initial_state() {
		assertTrue(subject.isOpen());
	}

	// behavior

	@Test
	public void close_delegated() throws Exception {
		subject.close();
		verify(channel).close();
	}

	@Test
	public void can_read_from_sufficiently_filled_channel() throws Exception {
		when(sync.limit()).thenReturn(10L);
		when(channel.read(any(ByteBuffer.class))).thenReturn(5);
		final ByteBuffer buf = ByteBuffer.allocate(5);
		assertEquals(5, subject.read(buf));
		verify(channel).read(buf);
	}

	@Test
	public void reading_above_current_limit_awaits_for_required_size()
			throws Exception {
		when(channel.read(any(ByteBuffer.class))).thenReturn(20);
		when(sync.limit()).thenReturn(10L);
		final ByteBuffer buf = ByteBuffer.allocate(20);
		assertEquals(20, subject.read(buf));
		verify(sync).await(20);
	}

	@Test
	public void limiter_keeps_track_of_read_bytes() throws Exception {
		when(channel.read(any(ByteBuffer.class))).thenReturn(10);
		final ByteBuffer buf = ByteBuffer.allocate(10);
		assertEquals(10, subject.read(buf));
		verify(sync).await(10);
		assertEquals(10, subject.read(buf));
		verify(sync).await(20);
	}
}
