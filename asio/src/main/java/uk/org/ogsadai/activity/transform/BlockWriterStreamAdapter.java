package uk.org.ogsadai.activity.transform;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.annotation.concurrent.ThreadSafe;

import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.PipeClosedException;
import uk.org.ogsadai.activity.io.PipeIOException;
import uk.org.ogsadai.activity.io.PipeTerminatedException;
import uk.org.ogsadai.activity.io.ProcessingIOException;
import uk.org.ogsadai.activity.io.TerminatedIOException;

/**
 * Adapt an enclosed {@link BlockWriter} to the {@link OutputStream} interface.
 * Bytes written to this stream are divided into fixed size byte arrays and
 * forwarded to the enclosed writer.
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class BlockWriterStreamAdapter extends OutputStream {

	private final BlockWriter writer;
	private final ByteBuffer buffer;
	private boolean closed;

	BlockWriterStreamAdapter(final BlockWriter writer, final int blockSize) {
		this.writer = checkNotNull(writer, "given BlockWriter is null");
		checkArgument(blockSize > 0,
				"blocksize must be greater than zero but was %s", blockSize);
		buffer = ByteBuffer.allocate(blockSize);
		closed = false;
	}

	@Override
	public synchronized void write(final int b) throws IOException {
		checkClosed();
		pushIfFull();
		buffer.put((byte) b);
	}

	@Override
	public synchronized void write(final byte[] b, final int off, final int len)
			throws IOException {
		checkClosed();
		checkNotNull(b);
		checkPositionIndexes(off, off + len, b.length);
		if (len == 0) {
			return;
		}
		int total = 0;
		int idx = off;
		do {
			pushIfFull();
			final int batch = Math.min(buffer.remaining(), len - total);
			buffer.put(b, idx, batch);
			idx += batch;
			total += batch;
		} while (total < len);
	}

	/**
	 * Note that the enclosed {@link BlockWriter} is <strong>not</strong>
	 * closed.
	 */
	@Override
	public synchronized void close() throws IOException {
		super.close();
		// empty buffer
		final byte[] block = copyAndClearBuffer();
		forward(block);
		closed = true;
	}

	private void checkClosed() throws IOException {
		if (closed) {
			throw new IOException("stream is closed");
		}
	}

	/**
	 * Forward the buffer's contents to the set {@link BlockWriter} and prepare
	 * the buffer for new writes.
	 * 
	 * @throws IOException
	 *             if writing to set BlockWriter fails
	 */
	private void pushIfFull() throws IOException {
		if (!buffer.hasRemaining()) {
			final byte[] block = copyAndClearBuffer();
			forward(block);
		}
	}

	/**
	 * @return new byte array with buffer's content
	 */
	private byte[] copyAndClearBuffer() {
		final byte[] block = new byte[buffer.position()];
		buffer.flip();
		buffer.get(block);
		buffer.clear();
		return block;
	}

	/**
	 * Write the given block to the set {@link BlockWriter}.
	 * 
	 * @param block
	 *            to be written
	 * @throws IOException
	 *             wrapping any BlockWriter error
	 */
	private void forward(final byte[] block) throws IOException {
		try {
			writer.write(block);
		} catch (PipeClosedException | PipeIOException e) {
			throw new ProcessingIOException(e);
		} catch (final PipeTerminatedException e) {
			throw new TerminatedIOException(e);
		}
	}
}
