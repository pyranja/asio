package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.ResultRepository;
import at.ac.univie.isc.asio.common.Disposable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Use temporary files in the given directory to store results.
 * 
 * @author Chris Borckholder
 */
public class FileResultRepository implements ResultRepository, Disposable {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(FileResultRepository.class);

	private final Path root;

	public FileResultRepository(final Path root) {
		this.root = root;
	}

	@Override
	public ResultHandler newHandlerFor(final DatasetOperation operation) {
		final Path file = root.resolve(Paths.get(operation.id()));
		try {
			Files.createFile(file);
		} catch (final IOException e) {
			throw new DatasetTransportException(e);
		}
		return new CompletionResultHandler(new FileResult(file), operation
				.format().asMediaType());
	}

	/**
	 * Create a new {@link ResultHandler} that uses temporary files in the set
	 * directory to store result data.
	 * 
	 * @return the created handler
	 * @throws DatasetTransportException
	 *             if file creation failed
	 */
	public ResultHandler newHandler(
			final SerializationFormat serializationFormat)
			throws DatasetTransportException {
		return new CompletionResultHandler(newResult(),
				serializationFormat.asMediaType());
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

	/**
	 * Delete the directory used by this repository and all of its contents.
	 * 
	 * @throws IOException
	 *             encountered while deleting
	 */
	@Override
	public void dispose() throws IOException {
		log.info("clearing result directory {}", root);
		Files.walkFileTree(root, new PurgeVisitor());
	}

	@Override
	public ListenableFuture<Result> find(final String opId) {
		throw new UnsupportedOperationException("find not implemented");
	}

	@Override
	public boolean delete(final String opId) {
		throw new UnsupportedOperationException("delete not implemented");
	}
}
