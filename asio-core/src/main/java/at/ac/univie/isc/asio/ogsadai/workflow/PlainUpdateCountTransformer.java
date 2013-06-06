package at.ac.univie.isc.asio.ogsadai.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import uk.org.ogsadai.activity.transform.BlockTransformer;

import com.google.common.base.Charsets;

/**
 * Convert the integer result of a SQLUpdate to plain text.
 * 
 * @author Chris Borckholder
 */
public class PlainUpdateCountTransformer implements BlockTransformer {

	private static final String TEMPLATE = "'%s' : %s rows updated";

	private final String query;

	PlainUpdateCountTransformer(final String query) {
		super();
		this.query = query;
	}

	@Override
	public void writeObject(final OutputStream sink, final Object block)
			throws IOException {
		checkNotNull(block, "invalid update count");
		checkArgument(block instanceof Integer,
				"update count (%s) not an integer", block);
		final Integer updateCount = (Integer) block;
		final String text = format(Locale.ENGLISH, TEMPLATE, query, updateCount);
		sink.write(text.getBytes(Charsets.UTF_8));
	}
}
