package at.ac.univie.isc.asio.transport;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class FileResultTest {

	private static final byte[] PAYLOAD = "HELLO WORLD! TEST"
			.getBytes(Charsets.UTF_8);

	private FileResult subject;
	private Path current;

	@Before
	public void setUp() throws IOException {
		current = Files.createTempFile("fileresulttest-", ".tmp");
		subject = new FileResult(current);
	}

	@After
	public void tearDown() throws IOException {
		Files.deleteIfExists(current);
	}

	@Test
	public void writes_to_backing_file() throws Exception {
		ByteStreams.write(PAYLOAD, subject);
		final byte[] read = Files.readAllBytes(current);
		assertArrayEquals(PAYLOAD, read);
	}

	@Test
	public void reads_from_backing_file() throws Exception {
		Files.write(current, PAYLOAD);
		final byte[] read = ByteStreams.toByteArray(subject);
		assertArrayEquals(PAYLOAD, read);
	}
}
