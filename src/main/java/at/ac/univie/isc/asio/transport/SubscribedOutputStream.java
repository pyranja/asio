package at.ac.univie.isc.asio.transport;

import com.google.common.base.Preconditions;
import rx.Subscriber;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public class SubscribedOutputStream extends OutputStream {

  private final Subscriber<? super byte[]> consumer;
  private ByteBuffer buffer;
  private boolean closed = false;

  public SubscribedOutputStream(final Subscriber<? super byte[]> consumer) {
    this.consumer = consumer;
    this.buffer = ByteBuffer.allocate(ObservableStream.MAX_CHUNK_SIZE);
  }

  @Override
  public void write(final byte[] data, final int off, final int len) throws IOException {
    requireNonNull(data);
    Preconditions.checkPositionIndexes(off, off + len, data.length);
    checkState();
    if (len == 0) {
      return;
    } else if (len <= buffer.remaining()) {
      buffer.put(data, off, len);
    } else {
      transfer(data, off, len);
    }
  }

  private void transfer(final byte[] data, final int off, final int len) throws IOException {
    final int dataMaxIndex = off + len;
    final ByteBuffer input = ByteBuffer.wrap(data, off, len);
    while (input.hasRemaining()) {
      flushIfFull();
      final int maxTransfer = Math.min(buffer.remaining(), input.remaining());
      input.limit(input.position() + maxTransfer);
      buffer.put(input);
      input.limit(dataMaxIndex);
    }
  }

  @Override
  public void write(final int b) throws IOException {
    checkState();
    flushIfFull();
    buffer.put((byte) b);
  }

  private void flushIfFull() throws IOException {
    if (!buffer.hasRemaining()) {
      flush();
    }
  }

  private void checkState() throws IOException {
    if (closed) {
      throw new IOException("closed");
    }
    if (consumer.isUnsubscribed()) {
      closed = true;
      throw new IOException("closed by unsubscribing");
    }
  }

  @Override
  public void flush() throws IOException {
    if (buffer.position() > 0) {  // skip if buffer is empty
      buffer.flip();
      final byte[] chunk = new byte[buffer.remaining()];
      buffer.get(chunk);
      buffer.clear();
      checkedForward(chunk);
    }
  }

  private void checkedForward(final byte[] chunk) throws IOException {
    try {
      consumer.onNext(chunk);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      flush();
      closed = true;
    } // ignore multiple close()
  }
}
