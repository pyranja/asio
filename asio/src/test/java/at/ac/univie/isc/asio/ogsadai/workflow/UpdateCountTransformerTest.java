package at.ac.univie.isc.asio.ogsadai.workflow;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXB;

import org.junit.Test;

import at.ac.univie.isc.asio.transfer.UpdateResult;

public class UpdateCountTransformerTest {

	@Test
	public void xml_transformation() throws Exception {
		final String query = "A TEST QUERY";
		final int count = 42;
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final XmlUpdateCountTransformer transformer = new XmlUpdateCountTransformer(
				query);
		transformer.writeObject(out, count);
		final ByteArrayInputStream xml = new ByteArrayInputStream(
				out.toByteArray());
		final UpdateResult transformed = JAXB
				.unmarshal(xml, UpdateResult.class);
		assertEquals(query, transformed.getCommand());
		assertEquals(count, transformed.getCount());
	}
}
