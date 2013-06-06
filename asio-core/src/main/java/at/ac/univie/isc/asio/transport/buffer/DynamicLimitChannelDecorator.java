package at.ac.univie.isc.asio.transport.buffer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

/**
 * Decorate a given {@link ReadableByteChannel} and throttle
 * {@link #read(ByteBuffer)} calls to never exceed the limit of given
 * {@link BufferSync}. If a read is initiated with a buffer whose remaining
 * capacity is greater than the current limit, the read will block until the
 * buffer has grown to a sufficient size or will not grow again.
 * 
 * @author Chris Borckholder
 */
class DynamicLimitChannelDecorator implements ReadableByteChannel {

	private final ReadableByteChannel delegate;
	private final BufferSync sync;
	// state
	private long read;

	DynamicLimitChannelDecorator(final ReadableByteChannel channel,
			final BufferSync sync) {
		delegate = checkNotNull(channel, "delegate channel cannot be null");
		this.sync = checkNotNull(sync, "buffer synchronizer cannot be null");
		read = 0;
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public synchronized int read(final ByteBuffer dst) throws IOException {
		final long required = read + dst.remaining();
		try {
			sync.await(required);
		} catch (final InterruptedException e) {
			throw new InterruptedIOException(format(Locale.ENGLISH,
					"interrupted while awaiting required size of %s", required));
		}
		final int readBytes = delegate.read(dst);
		read += readBytes;
		return readBytes;
	}
}
