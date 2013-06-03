package at.ac.univie.isc.asio.transport.channel;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Charsets;

@RunWith(MockitoJUnitRunner.class)
public class EventfulChannelTest {

	private static final ByteBuffer PAYLOAD = ByteBuffer.wrap("TEST1234"
			.getBytes(Charsets.UTF_8));
	private static final ByteBuffer SINK = ByteBuffer.allocate(64);

	private ByteChannel subject;
	@Mock private ByteChannel channel;
	@Mock private ChannelListener listener;

	@Before
	public void setUp() {
		SINK.clear();
		subject = EventfulChannel.decorate(channel, listener);
	}

	// invariances

	@Test(expected = NullPointerException.class)
	public void fail_on_null_channel() throws Exception {
		EventfulChannel.decorate((ByteChannel) null);
	}

	// notifications

	@Test
	public void notify_close() throws Exception {
		subject.close();
		verify(listener).onClose();
	}

	@Test
	public void do_not_notify_of_zero_write() throws Exception {
		when(channel.write(any(ByteBuffer.class))).thenReturn(0);
		subject.write(PAYLOAD);
		verifyZeroInteractions(listener);
	}

	@Test
	public void notify_on_non_zero_write() throws Exception {
		when(channel.write(any(ByteBuffer.class))).thenReturn(10);
		subject.write(PAYLOAD);
		verify(listener).onWrite(10);
	}

	@Test
	public void do_not_notify_on_zero_read() throws Exception {
		when(channel.read(any(ByteBuffer.class))).thenReturn(10);
		subject.read(SINK);
		verify(listener).onRead(10);
	}

	@Test
	public void notify_on_non_zero_read() throws Exception {
		when(channel.read(any(ByteBuffer.class))).thenReturn(10);
		subject.read(SINK);
		verify(listener).onRead(10);
	}

	// error notification

	@Test(expected = IOException.class)
	public void notify_close_error() throws Exception {
		final IOException cause = new IOException();
		doThrow(cause).when(channel).close();
		try {
			subject.close();
		} finally {
			verify(listener).onError(cause);
		}
	}

	@Test(expected = IOException.class)
	public void notify_write_error() throws Exception {
		final IOException cause = new IOException();
		doThrow(cause).when(channel).write(any(ByteBuffer.class));
		try {
			subject.write(PAYLOAD);
		} finally {
			verify(listener).onError(cause);
		}
	}

	@Test(expected = IOException.class)
	public void notify_read_error() throws Exception {
		final IOException cause = new IOException();
		doThrow(cause).when(channel).read(any(ByteBuffer.class));
		try {
			subject.read(SINK);
		} finally {
			verify(listener).onError(cause);
		}
	}

	// delegation

	@Test
	public void delegate_closing() throws Exception {
		subject.close();
		verify(channel).close();
	}

	@Test
	public void delegate_is_open() throws Exception {
		subject.isOpen();
		verify(channel).isOpen();
	}

	@Test
	public void delegate_write() throws Exception {
		subject.write(PAYLOAD);
		verify(channel).write(PAYLOAD);
	}

	@Test
	public void delegate_read() throws Exception {
		subject.read(SINK);
		verify(channel).read(SINK);
	}
}
