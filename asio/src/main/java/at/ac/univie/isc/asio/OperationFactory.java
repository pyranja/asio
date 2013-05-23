package at.ac.univie.isc.asio;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.String.format;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.common.IdGenerator;

import com.google.common.base.Preconditions;

/**
 * Creates and validates {@link DatasetOperation DatasetOperations}.
 * 
 * @author Chris Borckholder
 */
public class OperationFactory {

	private final IdGenerator ids;

	public OperationFactory(final IdGenerator ids) {
		super();
		this.ids = ids;
	}

	/**
	 * Create a QUERY dataset operation
	 * 
	 * @param query
	 *            to be executed
	 * @param format
	 *            for result rendering
	 * @return the parameterized operation
	 */
	public DatasetOperation query(final String query,
			final SerializationFormat format) {
		userErrorIfNull(emptyToNull(query), "illegal query %s", query);
		checkNotNull(format, "format is null");
		return new DatasetOperation(ids.next(), Action.QUERY, query, format);
	}

	/**
	 * Create a SCHEMA dataset operation
	 * 
	 * @param format
	 *            for result rendering
	 * @return the parameterized operation
	 */
	public DatasetOperation schema(final SerializationFormat format) {
		checkNotNull(format, "format is null");
		return new DatasetOperation(ids.next(), Action.SCHEMA, null, format);
	}

	/**
	 * Like {@link Preconditions#checkNotNull(Object, String, Object...)}, but
	 * throws {@link DatasetUsageException}.
	 */
	private <T> T userErrorIfNull(final T reference, final String format,
			final Object... parameters) throws DatasetUsageException {
		if (reference == null) {
			throw new DatasetUsageException(format(format, parameters));
		}
		return reference;
	}
}
