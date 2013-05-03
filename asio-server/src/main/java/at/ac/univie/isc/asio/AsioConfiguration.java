package at.ac.univie.isc.asio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import at.ac.univie.isc.asio.transport.FileResultRepository;

/**
 * Setup the asio endpoint infrastructure.
 * 
 * @author Chris Borckholder
 */
@Configuration
@ImportResource(value = { "classpath:/spring/asio-cxf.xml" })
public class AsioConfiguration {

	@Autowired DatasetEngine engine;

	@Bean(name = "asio_facade")
	public QueryEndpoint asioFacade() {
		return new SqlEndpoint(engine);
	}

	@Bean
	public FileResultRepository resultRepository() throws IOException {
		final Path resultsDirectory = Files
				.createTempDirectory("asio-results-");
		return new FileResultRepository(resultsDirectory);
	}
}
