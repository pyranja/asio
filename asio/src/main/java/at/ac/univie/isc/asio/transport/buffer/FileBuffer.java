package at.ac.univie.isc.asio.transport.buffer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Set;

import at.ac.univie.isc.asio.common.Disposable;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;

/**
 * A file based buffer, that offers a single {@link OutputStream} to append to
 * the buffer and multiple {@link InputStream}s to read the buffer's content in
 * parallel.
 * 
 * <p>
 * Streams provided by a buffer are not safe for concurrent usage. Concurrent
 * writes to the stream returned by {@link #asOutputStream()} must be
 * synchronized. Streams supplied by {@link #getInput()} are independent of each
 * other. The same applies to channels returned by {@link #asChannel()} and
 * {@link #newReadChannel()}.
 * </p>
 * <p>
 * This implementation internally uses {@link java.nio.Channel}s, therefore
 * using using the provided channels may avoid additional overhead.
 * </p>
 * 
 * @author Chris Borckholder
 */
public class FileBuffer implements Disposable, Closeable,
		InputSupplier<InputStream> {

	/**
	 * Factory method
	 * 
	 * @param backing
	 *            file to store temporary data
	 * @return an empty buffer
	 */
	public static FileBuffer create(final Path backing) {
		return new FileBuffer(backing);
	}

	private final static Set<OpenOption> BUFFER_WRITE_OPTIONS = ImmutableSet
			.<OpenOption> of(WRITE, CREATE, TRUNCATE_EXISTING);
	private final static Set<OpenOption> BUFFER_READ_OPTIONS = ImmutableSet
			.<OpenOption> of(READ);

	private final Path backing;
	private final BufferSync sync;
	// lazy initialized channel to buffer
	private WritableByteChannel toBuffer;

	FileBuffer(final Path file) {
		checkNotNull(file, "cannot use null as backing file");
		backing = file;
		sync = new BufferSync();
	}

	/**
	 * Return the channel that is capable of writing to this buffer.
	 * 
	 * @return an {@link WritableByteChannel} to this buffer
	 * @throws IOException
	 *             if creating or opening the backing file fails
	 */
	public synchronized WritableByteChannel asChannel() throws IOException {
		if (toBuffer == null) {
			toBuffer = channelToBackingFile();
		}
		return toBuffer;
	}

	/**
	 * Return the stream that is capable of writing to this buffer.
	 * 
	 * @return an {@link OutputStream} to this buffer
	 * @throws IOException
	 *             if creating or opening the backing file fails
	 */
	public synchronized OutputStream asOutputStream() throws IOException {
		return Channels.newOutputStream(asChannel());
	}

	/**
	 * @return buffer synced channel to set backing file
	 * @throws IOException
	 *             on file creation/open error
	 */
	private WritableByteChannel channelToBackingFile() throws IOException {
		final WritableByteChannel fileChannel = FileChannel.open(backing,
				BUFFER_WRITE_OPTIONS);
		return new CountingChannelDecorator(fileChannel, sync);
	}

	public ReadableByteChannel newReadChannel() throws IOException {
		return channelFromBackingFile();
	}

	@Override
	public InputStream getInput() throws IOException {
		return Channels.newInputStream(channelFromBackingFile());
	}

	/**
	 * @return buffer synced channel from set backing file
	 * @throws IOException
	 *             on file open error
	 */
	private ReadableByteChannel channelFromBackingFile() throws IOException {
		final ReadableByteChannel fileChannel = FileChannel.open(backing,
				BUFFER_READ_OPTIONS);
		if (sync.isGrowing()) { // only sync if writes to buffer possible
			return new DynamicLimitChannelDecorator(fileChannel, sync);
		} else {
			return fileChannel;
		}
	}

	@Override
	public void dispose() throws IOException {
		Closeables.close(this, true);
		try {
			Files.deleteIfExists(backing);
		} finally { // best effort fall back
			backing.toFile().deleteOnExit();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			if (toBuffer != null) {
				toBuffer.close();
			}
		} finally {
			sync.freeze(); // ensure sync is notified
		}
	}

	@Override
	public String toString() {
		return String.format("FileBuffer [backing=%s]", backing);
	}
}
