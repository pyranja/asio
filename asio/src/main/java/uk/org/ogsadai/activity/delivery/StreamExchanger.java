package uk.org.ogsadai.activity.delivery;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Optional;
import com.google.common.io.OutputSupplier;

/**
 * Provide a temporary container for id->{@link OutputStream} pairs. A stream is
 * stored as is until it is retrieved through its corresponding id. Specifically
 * closing the streams remains the duty of the users of this class. A stream can
 * only be retrieved once.
 * 
 * <p>
 * All methods of this class do <strong>not</strong> accept null parameters and
 * will throw NullPointerExceptions if any given parameter is null.
 * </p>
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class StreamExchanger {

	/**
	 * Throw if the given id is already associated with a stream. This indicates
	 * a logic error.
	 * 
	 * @author Chris Borckholder
	 */
	public static class IdTakenException extends IllegalStateException {
		private static final long serialVersionUID = -4694036673651816755L;

		private IdTakenException(final String id) {
			super(String.format(Locale.ENGLISH, "id %s is already taken", id));
		}
	}

	private final ConcurrentMap<String, OutputSupplier<OutputStream>> streams;

	/**
	 * Create an empty exchanger.
	 */
	public StreamExchanger() {
		super();
		streams = new ConcurrentHashMap<>();
	}

	/**
	 * Map the given id to the given {@link OutputStream}. Existing mappings may
	 * not be overwritten.
	 * 
	 * @param id
	 *            to be associated with given stream
	 * @param supplier
	 *            to be stored
	 * @throws IdTakenException
	 *             if the given id is already associated with a sink
	 */
	public void offer(final String id,
			final OutputSupplier<OutputStream> supplier)
			throws IdTakenException {
		checkNotNull(id, "invalid sink id : null");
		checkNotNull(supplier, "invalid supplier for id %s : null", id);
		final OutputSupplier<OutputStream> former = streams.putIfAbsent(id,
				supplier);
		if (former != null) {
			throw new IdTakenException(id);
		}
	}

	/**
	 * Retrieve the stream associated to the given id if one is present.
	 * 
	 * @param id
	 *            of required stream
	 * @return Optional containing the stream
	 */
	public Optional<OutputSupplier<OutputStream>> take(final String id) {
		checkNotNull(id, "invalid sink id : null");
		final OutputSupplier<OutputStream> stream = streams.remove(id);
		return Optional.fromNullable(stream);
	}

}
