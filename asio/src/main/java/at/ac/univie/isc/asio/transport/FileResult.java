package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;

/**
 * Enable writing and reading of streamed result data to/from a file.
 * 
 * @author Chris Borckholder
 */
public class FileResult implements Buffer {

	private final Path backing;

	FileResult(final Path backing) {
		super();
		this.backing = backing;
	}

	@Override
	public OutputStream getOutput() throws IOException {
		return Files.newOutputStream(backing);
	}

	@Override
	public InputStream getInput() throws IOException {
		return Files.newInputStream(backing);
	}

	@VisibleForTesting
	Path getBacking() {
		return backing;
	}
}
