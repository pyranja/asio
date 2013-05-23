package at.ac.univie.isc.asio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class TestUtils {

	/**
	 * Tests two strings for equality ignoring case.
	 * 
	 * @param expected
	 *            value
	 * @param given
	 *            real value
	 */
	public static void assertEqualsIgnoreCase(final String expected,
			final String given) {
		if (!expected.equalsIgnoreCase(given)) {
			throw new AssertionError("expected [" + expected + "] but was ["
					+ given + "]");
		}
	}

	/**
	 * @param response
	 *            to be printed
	 * @return the header and body of this response as text
	 */
	public static String stringify(final Response response) {
		final StringBuilder sb = new StringBuilder();
		sb.append("STATUS ").append(stringify(response.getStatusInfo()));
		sb.append("\nHEADER\n").append(response.getStringHeaders());
		sb.append("\nBODY\n");
		appendEntity(response.getEntity(), sb);
		sb.append("\nEND RESPONSE");
		return sb.toString();
	}

	/**
	 * append the given response entity to a StringBuilder. If the entity is an
	 * InputStream the stream is consumed and copied to the builder.
	 * 
	 * @param entity
	 *            to print
	 * @param to
	 *            builder to hold text data
	 * @return the given builder
	 */
	private static StringBuilder appendEntity(final Object entity,
			final StringBuilder to) {
		if (entity instanceof InputStream) {
			try (InputStreamReader body = new InputStreamReader(
					(InputStream) entity, Charsets.UTF_8)) {
				CharStreams.copy(body, to);
			} catch (final IOException e) {
				to.append("\n!io error on reading body!\n").append(
						e.getLocalizedMessage());
			}
		} else {
			to.append(entity);
		}
		return to;
	}

	/**
	 * @param status
	 *            to print
	 * @return status as text in format [status_code|status_class : reason]
	 */
	private static String stringify(final StatusType status) {
		return String.format(Locale.ENGLISH, "[%s|%s : %s]",
				status.getStatusCode(), status.getFamily(),
				status.getReasonPhrase());
	}
}
