package at.ac.univie.isc.asio.transport.buffer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Charsets;

@RunWith(MockitoJUnitRunner.class)
public class CountingChannelTest {

	private static final ByteBuffer PAYLOAD = ByteBuffer.wrap("HELLO WORLD!"
			.getBytes(Charsets.UTF_8));

	private CountingChannelDecorator subject;
	@Mock private WritableByteChannel channel;
	@Mock private BufferSync listener;

	@Before
	public void setUp() throws IOException {
		when(channel.isOpen()).thenReturn(true);
		subject = new CountingChannelDecorator(channel, listener);
	}

	// construction invariants
	@Test(expected = NullPointerException.class)
	public void null_channel_fails() {
		subject = new CountingChannelDecorator(null, listener);
	}

	@Test(expected = NullPointerException.class)
	public void null_listener_fails() {
		subject = new CountingChannelDecorator(channel, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void construct_from_closed_channel_fails() throws IOException {
		when(channel.isOpen()).thenReturn(false);
		subject = new CountingChannelDecorator(channel, listener);
	}

	// behavior

	@Test
	public void closing_closes_delegate() throws IOException {
		subject.close();
		verify(channel).close();
	}

	@Test
	public void closing_triggers_close_event() throws IOException {
		subject.close();
		verify(listener).freeze();
	}

	@Test
	public void writing_zero_bytes_triggers_no_count() throws IOException {
		subject.write(ByteBuffer.wrap(new byte[0]));
		verifyZeroInteractions(listener);
	}

	@Test
	public void writes_propagated_to_delegate() throws IOException {
		subject.write(PAYLOAD);
		verify(channel).write(PAYLOAD);
	}

	@Test
	public void nonzero_write_count_triggers_event() throws IOException {
		when(channel.write(PAYLOAD)).thenReturn(PAYLOAD.capacity());
		subject.write(PAYLOAD);
		verify(listener).grow(PAYLOAD.capacity());
	}
}
