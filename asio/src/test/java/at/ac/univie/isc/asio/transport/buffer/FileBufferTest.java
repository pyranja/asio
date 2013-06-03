package at.ac.univie.isc.asio.transport.buffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class FileBufferTest {

	private static final byte[] PAYLOAD = "HELLO WORLD!THIS IS A BUFFER TEST"
			.getBytes(Charsets.UTF_8);

	private FileBuffer subject;
	private Path current_file;

	private ExecutorService exec;

	@Before
	public void setUp() throws IOException {
		current_file = Files.createTempFile("test-", ".buf");
		subject = new FileBuffer(current_file);
		exec = Executors.newCachedThreadPool();
	}

	@After
	public void tearDown() throws IOException {
		exec.shutdownNow();
		Files.deleteIfExists(current_file);
	}

	// construction invariances

	@Test(expected = NullPointerException.class)
	public void construct_from_null_path_fails() {
		new FileBuffer(null);
	}

	// behavior

	@Test
	@Ignore
	// not true anymore - but streams wrap the same channel
	// -> see uses_single_write_channel
	public void uses_single_outputstream() throws IOException {
		try (OutputStream stream = subject.asOutputStream()) {
			assertSame(stream, subject.asOutputStream());
		}
	}

	@Test
	public void uses_single_write_channel() throws IOException {
		try (Channel channel = subject.asChannel()) {
			assertSame(channel, subject.asChannel());
		}
	}

	@Test
	public void writes_through_to_file() throws IOException {
		try (final OutputStream toBuffer = subject.asOutputStream()) {
			toBuffer.write(PAYLOAD);
		}
		assertArrayEquals(PAYLOAD, Files.readAllBytes(current_file));
	}

	@Test
	public void reads_from_backing_file() throws IOException {
		try (final OutputStream toBuffer = subject.asOutputStream()) {
			toBuffer.write(PAYLOAD);
		}
		final InputStream fromBuffer = subject.getInput();
		final byte[] read = new byte[PAYLOAD.length];
		assertEquals(PAYLOAD.length, fromBuffer.read(read));
		fromBuffer.close();
		assertArrayEquals(PAYLOAD, read);
	}

	@Test(timeout = 1000)
	public void reader_blocks_if_writes_possible() throws InterruptedException,
			ExecutionException {
		final FileBuffer shared = subject;
		final AtomicBoolean blocking = new AtomicBoolean(true);
		final CountDownLatch reader_started = new CountDownLatch(1);
		final Future<byte[]> reader = exec.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				reader_started.countDown();
				final byte[] read = ByteStreams.toByteArray(shared);
				blocking.set(false);
				return read;
			}
		});
		reader_started.await();
		Thread.sleep(10);
		assertTrue("reader not blocking", blocking.get());
		final Future<Void> writer = exec.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try (final OutputStream toBuffer = shared.asOutputStream()) {
					toBuffer.write(PAYLOAD);
				}
				return null;
			}
		});
		writer.get();
		final byte[] read = reader.get();
		assertArrayEquals(PAYLOAD, read);
	}

	@Test(timeout = 1000)
	public void closing_writer_interrupts_reader_wait()
			throws InterruptedException, IOException, ExecutionException {
		final FileBuffer shared = subject;
		final AtomicBoolean blocking = new AtomicBoolean(true);
		final CountDownLatch reader_started = new CountDownLatch(1);
		final Future<byte[]> reader = exec.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				reader_started.countDown();
				final byte[] read = ByteStreams.toByteArray(shared);
				blocking.set(false);
				return read;
			}
		});
		reader_started.await();
		Thread.sleep(10);
		assertTrue("reader not blocking", blocking.get());
		final OutputStream sharedToBuffer = shared.asOutputStream();
		final Future<Void> writer = exec.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				sharedToBuffer.write(PAYLOAD);
				return null;
			}
		});
		writer.get();
		Thread.sleep(10);
		assertTrue("reader broke blocking after write", blocking.get());
		final Future<Void> closer = exec.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				sharedToBuffer.close();
				return null;
			}
		});
		closer.get();
		final byte[] read = reader.get();
		assertArrayEquals(PAYLOAD, read);
	}

	@Test(timeout = 100)
	public void closing_buffer_interrupts_reader_wait() throws Exception {
		final FileBuffer shared = subject;
		final AtomicBoolean blocking = new AtomicBoolean(true);
		final CountDownLatch reader_started = new CountDownLatch(1);
		final Future<byte[]> reader = exec.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				reader_started.countDown();
				final byte[] read = ByteStreams.toByteArray(shared);
				blocking.set(false);
				return read;
			}
		});
		reader_started.await();
		Thread.sleep(10);
		assertTrue("reader not blocking", blocking.get());
		shared.close();
		final byte[] read = reader.get();
		assertArrayEquals(new byte[0], read);
	}

	@Test
	public void disposing_buffer_deletes_backing_file() throws IOException {
		subject.asOutputStream(); // force file creation
		assertTrue(Files.exists(current_file));
		subject.dispose();
		assertFalse(Files.exists(current_file));
	}
}
