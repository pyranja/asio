package at.ac.univie.isc.asio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.DatasetExceptionMapper;
import at.ac.univie.isc.asio.frontend.FrontendEngineAdapter;
import at.ac.univie.isc.asio.frontend.LogContextFilter;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.QueryEndpoint;
import at.ac.univie.isc.asio.frontend.SchemaEndpoint;
import at.ac.univie.isc.asio.frontend.UpdateEndpoint;
import at.ac.univie.isc.asio.frontend.VariantConverter;
import at.ac.univie.isc.asio.transport.FileResultRepository;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Setup the asio endpoint infrastructure.
 * 
 * @author Chris Borckholder
 */
@Configuration
@ImportResource(value = { "classpath:/spring/asio-cxf.xml" })
public class AsioConfiguration {

	// asio backend components

	@Autowired DatasetEngine engine;

	@Bean(destroyMethod = "dispose")
	public ResultRepository resultRepository() throws IOException {
		final Path resultsDirectory = Files
				.createTempDirectory("asio-results-");
		return new FileResultRepository(resultsDirectory);
	}

	// JAX-RS service endpoints

	@Bean(name = "asio_query")
	public QueryEndpoint queryService() {
		return new QueryEndpoint(engineAdapter(), processor(),
				operationFactory());
	}

	@Bean(name = "asio_schema")
	public SchemaEndpoint schemaService() {
		return new SchemaEndpoint(engineAdapter(), processor(),
				operationFactory());
	}

	@Bean(name = "asio_update")
	public UpdateEndpoint updateService() {
		return new UpdateEndpoint(engineAdapter(), processor(),
				operationFactory());
	}

	// JAX-RS provider

	@Bean(name = "asio_error_mapper")
	public DatasetExceptionMapper errorMapper() {
		return new DatasetExceptionMapper();
	}

	@Bean(name = "asio_log_filter")
	public LogContextFilter logFilter() {
		return new LogContextFilter();
	}

	// asio frontend components

	@Bean
	public OperationFactory operationFactory() {
		return new OperationFactory(RandomIdGenerator.withPrefix("asio"));
	}

	@Bean
	public FrontendEngineAdapter engineAdapter() {
		return new FrontendEngineAdapter(engine, converter());
	}

	@Bean
	public AsyncProcessor processor() {
		return new AsyncProcessor(responseExecutor(), converter());
	}

	@Bean(destroyMethod = "shutdown")
	public ExecutorService responseExecutor() {
		final ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat(
				"response-processer-%d").build();
		return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5,
				factory));
	}

	@Bean
	public VariantConverter converter() {
		return new VariantConverter();
	}
}
