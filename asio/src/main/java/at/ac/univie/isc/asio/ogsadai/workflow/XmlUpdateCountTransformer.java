package at.ac.univie.isc.asio.ogsadai.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import uk.org.ogsadai.activity.transform.BlockTransformer;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.transfer.UpdateResult;

import com.google.common.base.Charsets;

/**
 * Translate Integer blocks to {@link UpdateResult update responses} for the set
 * query and write them as XML document to the given stream.
 * 
 * @author Chris Borckholder
 */
public class XmlUpdateCountTransformer implements BlockTransformer {

	private static final JAXBContext JAXB;

	static {
		try {
			JAXB = JAXBContext.newInstance(UpdateResult.class);
		} catch (final JAXBException e) {
			throw new IllegalStateException("failed to initialize JAXBContext",
					e);
		}
	}

	private final String query;

	XmlUpdateCountTransformer(final String query) {
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
		final UpdateResult dto = new UpdateResult().withCommand(query)
				.withCount(updateCount);
		serialize(sink, dto);
	}

	private void serialize(final OutputStream sink, final UpdateResult dto) {
		try {
			final Marshaller marshaller = JAXB.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING,
					Charsets.UTF_8.name());
			marshaller.marshal(dto, sink);
		} catch (final JAXBException e) {
			throw new DatasetTransportException(e);
		}
	}
}
