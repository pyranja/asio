package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.net.MediaType;

public final class MockFormat {

	/**
	 * format that is applicable to any action with media type
	 * {@link #APPLICABLE_MIME}.
	 */
	public static final SerializationFormat ALWAYS_APPLICABLE = new SerializationFormat() {
		@Override
		public MediaType asMediaType() {
			return APPLICABLE_MIME;
		}

		@Override
		public boolean applicableOn(final Action action) {
			return true;
		}
	};

	/**
	 * format that is not applicable to any action with media type
	 * {@link #NOT_APPLICABLE_MIME}.
	 */
	public static SerializationFormat NEVER_APPLICABLE = new SerializationFormat() {
		@Override
		public MediaType asMediaType() {
			return NOT_APPLICABLE_MIME;
		}

		@Override
		public boolean applicableOn(final Action action) {
			return false;
		}
	};

	public static final javax.ws.rs.core.MediaType APPLICABLE_CONTENT_TYPE = javax.ws.rs.core.MediaType
			.valueOf("application/applicable");
	public static final javax.ws.rs.core.MediaType NOT_APPLICABLE_CONTENT_TYPE = javax.ws.rs.core.MediaType
			.valueOf("application/notapplicable");
	public static final MediaType APPLICABLE_MIME = MediaType.create(
			"application", "applicable");
	public static final MediaType NOT_APPLICABLE_MIME = MediaType.create(
			"application", "notapplicable");

	private MockFormat() {}
}