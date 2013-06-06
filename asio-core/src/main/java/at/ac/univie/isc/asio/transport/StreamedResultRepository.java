package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.ResultHandler;
import at.ac.univie.isc.asio.ResultRepository;
import at.ac.univie.isc.asio.common.Disposable;
import at.ac.univie.isc.asio.common.Resources;
import at.ac.univie.isc.asio.transport.buffer.FileBuffer;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Use file-based FIFO buffers to stream result data. Buffer files are created
 * in the directory given on creation.
 * 
 * @author Chris Borckholder
 */
public class StreamedResultRepository implements ResultRepository, Disposable {

	private final Path root;

	public StreamedResultRepository(final Path root) {
		super();
		this.root = root;
	}

	@Override
	public ResultHandler newHandlerFor(final DatasetOperation operation)
			throws DatasetTransportException {
		final Path file = root.resolve(operation.id());
		final FileBuffer buffer = FileBuffer.create(file);
		final Result expected = new BufferResult(buffer, operation.format()
				.asMediaType());
		try {
			return new StreamResultHandler(buffer.asOutputStream(), expected);
		} catch (final IOException e) {
			Resources.dispose(buffer);
			throw new DatasetTransportException(e);
		}
	}

	@Override
	public ListenableFuture<Result> find(final String opId)
			throws DatasetTransportException, DatasetUsageException {
		throw new UnsupportedOperationException("find not implemented");
	}

	@Override
	public boolean delete(final String opId) {
		throw new UnsupportedOperationException("delete not implemented");
	}

	/**
	 * Delete the directory used by this repository and all of its contents.
	 * 
	 * @throws IOException
	 *             encountered while deleting
	 */
	@Override
	public void dispose() throws IOException {
		Files.walkFileTree(root, new PurgeVisitor());
	}
}
