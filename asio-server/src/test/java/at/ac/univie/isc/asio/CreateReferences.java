package at.ac.univie.isc.asio;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

@Ignore
public class CreateReferences {

	/* slf4j-logger */
	final static Logger log = LoggerFactory.getLogger(CreateReferences.class);

	private WebClient client;

	@Before
	public void setUp() {
		client = WebClient.create("http://localhost:8080/v1/asio/query");
	}

	@After
	public void tearDown() {
		client.reset();
	}

	@Test
	public void personFullCsv() throws IOException {
		client.accept(
				MediaType.valueOf("text/csv")
						.withCharset(Charsets.UTF_8.name())).query("query",
				"SELECT * FROM person");
		final Response response = client.get();
		assertEquals(200, response.getStatus());
		final Path target = Paths.get("person_full.csv");
		try (InputStream body = (InputStream) response.getEntity()) {
			Files.copy(body, target);
		}
		log.info("created {}", target);
	}

	@Test
	public void personFullXml() throws IOException {
		client.accept(
				MediaType.valueOf("application/xml").withCharset(
						Charsets.UTF_8.name())).query("query",
				"SELECT * FROM person");
		final Response response = client.get();
		assertEquals(200, response.getStatus());
		final Path target = Paths.get("person_full.xml");
		try (InputStream body = (InputStream) response.getEntity()) {
			Files.copy(body, target);
		}
		log.info("created {}", target);
	}
}
