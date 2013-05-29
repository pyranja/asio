package uk.org.ogsadai.activity.delivery;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Optional;

/**
 * Provide a temporary, type and thread-safe container for id->object pairs.
 * Passing an identifier to {@link #offer(String, Object)} that is currently in
 * use is an error. A stored object may be retrieved exactly once using its
 * associated id and {@link #take(String)}.
 * 
 * <p>
 * All methods of this class do <strong>not</strong> accept null parameters and
 * will throw NullPointerExceptions if any given parameter is null.
 * </p>
 * 
 * @param T
 *            type of exchanged objects.
 * @author Chris Borckholder
 */
@ThreadSafe
@Deprecated
public class ObjectExchanger<T> {

	/**
	 * Throw if the given id is already associated with an object. This
	 * indicates a logic error.
	 * 
	 * @author Chris Borckholder
	 */
	public static class IdTakenException extends IllegalStateException {
		private static final long serialVersionUID = -4694036673651816755L;

		private IdTakenException(final String id) {
			super(String.format(Locale.ENGLISH, "id %s is already taken", id));
		}
	}

	private final ConcurrentMap<String, T> stored;

	/**
	 * Create an empty exchanger.
	 */
	public ObjectExchanger() {
		super();
		stored = new ConcurrentHashMap<>();
	}

	/**
	 * Map the given id to the given object of (sub)type (of) T. Existing
	 * mappings may not be overwritten.
	 * 
	 * @param id
	 *            to be associated with given stream
	 * @param that
	 *            object to be stored
	 * @throws IdTakenException
	 *             if the given id is already associated with an object
	 */
	public <U extends T> void offer(final String id, final U that)
			throws IdTakenException {
		checkNotNull(id, "invalid exchange identifier : null");
		checkNotNull(that, "invalid exchange object for id %s : null", id);
		final T former = stored.putIfAbsent(id, that);
		if (former != null) {
			throw new IdTakenException(id);
		}
	}

	/**
	 * Retrieve the object associated to the given id if one is present.
	 * 
	 * @param id
	 *            of required stream
	 * @return Optional containing the object
	 */
	public Optional<T> take(final String id) {
		checkNotNull(id, "invalid sink id : null");
		final T taken = stored.remove(id);
		return Optional.fromNullable(taken);
	}

}
