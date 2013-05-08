package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.ResultHandler;

import com.google.common.annotations.VisibleForTesting;

/**
 * Use temporary files in the given directory to store results.
 * 
 * @author Chris Borckholder
 */
public class FileResultRepository {

	private final Path root;

	public FileResultRepository(final Path root) {
		this.root = root;
	}

	public ResultHandler newHandler() throws DatasetTransportException {
		return new CompletionResultHandler(newResult());
	}

	@VisibleForTesting
	FileResult newResult() {
		Path resultFile;
		try {
			resultFile = Files.createTempFile(root, "asio-", ".result");
		} catch (final IOException e) {
			throw new DatasetTransportException(e);
		}
		return new FileResult(resultFile);
	}

	public void dispose() throws IOException {
		Files.walkFileTree(root, new PurgeVisitor());
	}
}
