package at.ac.univie.isc.asio;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;

/**
 * Represent an operation on a Dataset, including the command to be executed and
 * the required output format.
 * 
 * @author Chris Borckholder
 */
public class DatasetOperation {

	/**
	 * Marker interface for result output media types.
	 * 
	 * @author Chris Borckholder
	 */
	public static interface SerializationFormat {
		/**
		 * @return the common internet media type of this format
		 */
		MediaType asMediaType();
	}

	private final Optional<String> command;
	private final SerializationFormat format;

	private DatasetOperation(@Nullable final String command,
			final SerializationFormat format) {
		super();
		this.command = Optional.fromNullable(command);
		this.format = format;
	}

	/**
	 * The command to be executed by this operation if one is given.
	 * 
	 * @return optional holding the command if one is set
	 */
	public Optional<String> command() {
		return command;
	}

	/**
	 * The output format in which the results of this operation must be
	 * rendered.
	 * 
	 * @return the desired format
	 */
	public SerializationFormat format() {
		return format;
	}
}
