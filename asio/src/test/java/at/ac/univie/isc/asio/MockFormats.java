package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.net.MediaType;

public final class MockFormats {

	public static final javax.ws.rs.core.MediaType APPLICABLE_CONTENT_TYPE = javax.ws.rs.core.MediaType
			.valueOf("application/applicable");
	public static final javax.ws.rs.core.MediaType NOT_APPLICABLE_CONTENT_TYPE = javax.ws.rs.core.MediaType
			.valueOf("application/notapplicable");
	public static final MediaType APPLICABLE_MIME = MediaType.create(
			"application", "applicable");
	public static final MediaType NOT_APPLICABLE_MIME = MediaType.create(
			"application", "notapplicable");

	/**
	 * @return format that is not applicable to any action with media type
	 *         {@link #NOT_APPLICABLE_MIME}.
	 */
	public static SerializationFormat thatIsNeverApplicable() {
		return new SerializationFormat() {
			@Override
			public MediaType asMediaType() {
				return NOT_APPLICABLE_MIME;
			}

			@Override
			public boolean applicableOn(final Action action) {
				return false;
			}
		};
	}

	/**
	 * @return format that is applicable to any action with media type
	 *         {@link #APPLICABLE_MIME}.
	 */
	public static SerializationFormat thatIsAlwaysApplicable() {
		return new SerializationFormat() {
			@Override
			public MediaType asMediaType() {
				return APPLICABLE_MIME;
			}

			@Override
			public boolean applicableOn(final Action action) {
				return true;
			}
		};
	}

	private MockFormats() {}
}