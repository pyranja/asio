package at.ac.univie.isc.asio.ogsadai;

import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;

/**
 * {@link SerializationFormat}s supported by the {@link OgsadaiEngine}.
 * 
 * @author Chris Borckholder
 */
public enum OgsadaiFormats implements SerializationFormat {

	/**
	 * webrowset representation
	 */
	XML(MediaType.create("application", "xml").withCharset(Charsets.UTF_8)),
	/**
	 * tabular, comma-separated representation
	 */
	CSV(MediaType.CSV_UTF_8);

	private static final Set<SerializationFormat> valueSet = ImmutableSet
			.<SerializationFormat> copyOf(OgsadaiFormats.values());

	public static Set<SerializationFormat> asSet() {
		return valueSet;
	}

	private final MediaType type;

	private OgsadaiFormats(final MediaType type) {
		this.type = type;
	}

	@Override
	public MediaType asMediaType() {
		return type;
	}

}
