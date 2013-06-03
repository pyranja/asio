package at.ac.univie.isc.asio.transport.buffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Decorate a {@link WriteableByteChannel} by notifying the given
 * {@link BufferWriteListener} whenever bytes were successfully written to the
 * backing channel or the channel is closed.
 * 
 * <p>
 * This implementation is not synchronized, thread safety depends on the nature
 * of the backing channel and channel listener.
 * </p>
 * 
 * @author Chris Borckholder
 */
class CountingChannelDecorator implements WritableByteChannel {

	private final WritableByteChannel delegate;
	private final BufferSync listener;

	CountingChannelDecorator(final WritableByteChannel channel,
			final BufferSync listener) {
		checkNotNull(channel, "cannot use null as backing channel");
		checkNotNull(listener, "cannot use null as channel listener");
		checkArgument(channel.isOpen(), "backing channel is closed");
		delegate = channel;
		this.listener = listener;
	}

	/**
	 * <p>
	 * Triggers {@link BufferWriteListener.channelClosed()}
	 * </p>
	 */
	@Override
	public void close() throws IOException {
		try {
			delegate.close();
		} finally {
			listener.freeze();
		}
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	/**
	 * <p>
	 * Triggers {@link BufferWriteListener.channelWrite()} if at least one byte
	 * was written.
	 * </p>
	 */
	@Override
	public int write(final ByteBuffer src) throws IOException {
		final int written = delegate.write(src);
		if (written > 0) {
			listener.grow(written);
		}
		return written;
	}

	@Override
	public String toString() {
		return String.format(
				"CountingChannelDecorator [delegate=%s, listener=%s]",
				delegate, listener);
	}
}
