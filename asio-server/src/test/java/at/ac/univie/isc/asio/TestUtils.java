package at.ac.univie.isc.asio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.core.Response;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class TestUtils {

	/**
	 * @param response
	 *            to be printed
	 * @return the header and body of this response as text
	 */
	public static String toString(final Response response) {
		final StringBuilder sb = new StringBuilder();
		sb.append("ERROR STATUS [").append(response.getStatus()).append("|")
				.append(response.getStatusInfo()).append("]");
		sb.append("\nHEADER\n").append(response.getStringHeaders());
		sb.append("\nBODY\n");
		if (response.getEntity() instanceof InputStream) {
			try (InputStreamReader body = new InputStreamReader(
					(InputStream) response.getEntity(), Charsets.UTF_8)) {
				CharStreams.copy(body, sb);
			} catch (final IOException e) {
				sb.append("\n!io error on reading body!\n").append(
						e.getStackTrace());
			}
		} else {
			sb.append(response.getEntity());
		}
		sb.append("\nEND RESPONSE");
		return sb.toString();
	}
}
