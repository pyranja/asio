package at.ac.univie.isc.asio.transport;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileResultRepositoryTest {

	private FileResultRepository subject;
	private Path root;

	@Before
	public void setUp() throws IOException {
		root = Files.createTempDirectory("asio-repository-test");
		subject = new FileResultRepository(root);
	}

	@After
	public void tearDown() throws IOException {
		Files.walkFileTree(root, new PurgeVisitor());
	}

	@Test
	public void produced_handler_has_existing_file() throws Exception {
		final FileResult handler = subject.newResult();
		assertTrue(Files.exists(handler.getBacking()));
	}
}
