package at.ac.univie.isc.asio.frontend;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.String.format;

import javax.annotation.Nullable;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
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
	 * Create a QUERY dataset operation builder
	 * 
	 * @param query
	 *            to be executed
	 * @return a partial operation builder
	 */
	public OperationBuilder query(final String query) {
		userErrorIfNull(emptyToNull(query), "illegal query %s", query);
		return new OperationBuilder(ids.next(), Action.QUERY, query);
	}

	/**
	 * Create a SCHEMA dataset operation builder
	 * 
	 * @return a partial operation builder
	 */
	public OperationBuilder schema() {
		return new OperationBuilder(ids.next(), Action.SCHEMA, null);
	}

	/**
	 * Create an UPDATE dataset operation builder
	 * 
	 * @param update
	 *            to be executed
	 * @return a partial operation builder
	 */
	public OperationBuilder update(final String update) {
		userErrorIfNull(emptyToNull(update), "illegal update %s", update);
		return new OperationBuilder(ids.next(), Action.UPDATE, update);
	}

	/**
	 * Hold the partial state of a {@link DatasetOperation} in construction.
	 * 
	 * @author Chris Borckholder
	 */
	public static class OperationBuilder {

		private final String id;
		private final Action action;
		private final String command;

		private OperationBuilder(final String id, final Action action,
				@Nullable final String command) {
			this.id = id;
			this.action = action;
			this.command = command;
		}

		/**
		 * Supply the {@link SerializationFormat} to complete the construction
		 * of the operation
		 * 
		 * @param format
		 *            for results rendering
		 * @return the parameterized operation
		 */
		public DatasetOperation renderAs(final SerializationFormat format) {
			checkNotNull(format, "format is null");
			return new DatasetOperation(id, action, command, format);
		}

		@Override
		public String toString() {
			return String.format(
					"OperationBuilder [id=%s, action=%s, command=%s]", id,
					action, command);
		}
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
