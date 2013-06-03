package at.ac.univie.isc.asio.transport.channel;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.VisibleForTesting;

/**
 * Decorate a {@link ByteChannel} by notifying registered
 * {@link ChannelListener listeners} about actions on the enclosed channel.
 * <p>
 * Event notification and listener (de-)registration are thread-safe. The
 * thread-safety of channel operations depends on the nature of the backing
 * channel.
 * </p>
 * <p>
 * All static factory methods will check whether the given channel is already
 * eventful and attach to it instead wrapping it.
 * </p>
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public abstract class EventfulChannel<T extends Channel> implements Channel {

	/**
	 * @param channel
	 *            to be decorated
	 * @param listeners
	 *            to be notified
	 * @return a decorated readable byte channel
	 */
	public static ReadableByteChannel decorate(
			final ReadableByteChannel channel,
			final ChannelListener... listeners) {
		if (attachIfEventful(channel, listeners)) {
			return channel;
		} else {
			final EventfulReadableChannel product = new EventfulReadableChannel(
					channel);
			attachIfEventful(product, listeners);
			return product;
		}
	}

	/**
	 * @param channel
	 *            to be decorated
	 * @param listeners
	 *            to be notified
	 * @return a decorated writable byte channel
	 */
	public static WritableByteChannel decorate(
			final WritableByteChannel channel,
			final ChannelListener... listeners) {
		if (attachIfEventful(channel, listeners)) {
			return channel;
		} else {
			final EventfulWritableChannel product = new EventfulWritableChannel(
					channel);
			attachIfEventful(product, listeners);
			return product;
		}
	}

	/**
	 * @param channel
	 *            to be decorated
	 * @param listeners
	 *            to be notified
	 * @return a decorated byte channel
	 */
	public static ByteChannel decorate(final ByteChannel channel,
			final ChannelListener... listeners) {
		if (attachIfEventful(channel, listeners)) {
			return channel;
		} else {
			final EventfulByteChannel product = new EventfulByteChannel(channel);
			attachIfEventful(product, listeners);
			return product;
		}
	}

	/* reuse channel if already eventful */
	private static boolean attachIfEventful(final Channel channel,
			final ChannelListener... listeners) {
		if (channel instanceof EventfulChannel) {
			((EventfulChannel<?>) channel).listeners.addAll(asList(listeners));
			return true;
		}
		return false;
	}

	protected final T delegate;
	protected final Set<ChannelListener> listeners;

	@VisibleForTesting
	EventfulChannel(@Nonnull final T delegate) {
		super();
		this.delegate = checkNotNull(delegate, "delegate channel is null");
		listeners = new CopyOnWriteArraySet<>();
	}

	/* forwarding channel impl */

	@Override
	public final boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public final void close() throws IOException {
		try {
			delegate.close();
		} catch (final IOException e) {
			broadcast(e);
			throw e;
		} finally {
			for (final ChannelListener each : listeners) {
				each.onClose();
			}
		}
	}

	protected final void broadcast(final IOException cause) {
		for (final ChannelListener each : listeners) {
			each.onError(cause);
		}
	}

	/*
	 * Concrete implementations for read-only, write-only and read-write
	 * channels. Duplication needed here to circumvent multi-inheritance.
	 */

	private final static class EventfulReadableChannel extends
			EventfulChannel<ReadableByteChannel> implements ReadableByteChannel {

		EventfulReadableChannel(final ReadableByteChannel delegate) {
			super(delegate);
		}

		@Override
		public int read(final ByteBuffer dst) throws IOException {
			try {
				final int count = delegate.read(dst);
				if (count > 0) {
					for (final ChannelListener each : listeners) {
						each.onRead(count);
					}
				}
				return count;
			} catch (final IOException e) {
				broadcast(e);
				throw e;
			}
		}
	}

	private final static class EventfulWritableChannel extends
			EventfulChannel<WritableByteChannel> implements WritableByteChannel {

		EventfulWritableChannel(final WritableByteChannel delegate) {
			super(delegate);
		}

		@Override
		public int write(final ByteBuffer src) throws IOException {
			try {
				final int count = delegate.write(src);
				if (count > 0) {
					for (final ChannelListener each : listeners) {
						each.onWrite(count);
					}
				}
				return count;
			} catch (final IOException e) {
				broadcast(e);
				throw e;
			}
		}
	}

	private final static class EventfulByteChannel extends
			EventfulChannel<ByteChannel> implements ByteChannel {

		EventfulByteChannel(final ByteChannel delegate) {
			super(delegate);
		}

		@Override
		public int read(final ByteBuffer dst) throws IOException {
			try {
				final int count = delegate.read(dst);
				if (count > 0) {
					for (final ChannelListener each : listeners) {
						each.onRead(count);
					}
				}
				return count;
			} catch (final IOException e) {
				broadcast(e);
				throw e;
			}
		}

		@Override
		public int write(final ByteBuffer src) throws IOException {
			try {
				final int count = delegate.write(src);
				if (count > 0) {
					for (final ChannelListener each : listeners) {
						each.onWrite(count);
					}
				}
				return count;
			} catch (final IOException e) {
				broadcast(e);
				throw e;
			}
		}
	}

}
