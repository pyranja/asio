package at.ac.univie.isc.asio.ogsadai;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import uk.org.ogsadai.activity.event.RequestEventRouter;
import uk.org.ogsadai.common.ID;
import uk.org.ogsadai.context.OGSADAIContext;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.ResourceManager;
import uk.org.ogsadai.resource.ResourceType;
import uk.org.ogsadai.resource.ResourceUnknownException;
import uk.org.ogsadai.resource.drer.DRER;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.ResultRepository;
import at.ac.univie.isc.asio.ogsadai.workflow.SqlComposer;

import com.google.common.collect.Iterables;

/**
 * Setup asio connected to an in-process OGSADAI instance.
 * 
 * @author Chris Borckholder
 */
@Configuration
public class AsioOgsadaiConfiguration {

	@Autowired Environment env;

	private static final ID ROUTER_ID = new ID(
			"uk.org.ogsadai.MONITORING_FRAMEWORK");

	@Autowired ResultRepository resultRepository;

	@Bean
	public DatasetEngine ogsadaiEngine() {
		return new OgsadaiEngine(adapter(), resultRepository, composer(),
				translator());
	}

	@Bean
	public DaiExceptionTranslator translator() {
		return new DaiExceptionTranslator();
	}

	@Bean
	public WorkflowComposer composer() {
		final String resourceName = env
				.getRequiredProperty("asio.ogsadai.resource");
		final ResourceID resource = new ResourceID(resourceName, "");
		return new SqlComposer(resource);
	}

	@Bean
	public OgsadaiAdapter adapter() {
		final OGSADAIContext context = OGSADAIContext.getInstance();
		final RequestEventRouter router = (RequestEventRouter) context
				.get(ROUTER_ID);
		final DRER drer = findDRER(context);
		return new OgsadaiAdapter(drer, router);
	}

	private DRER findDRER(final OGSADAIContext context) {
		final ResourceManager resourceManager = context.getResourceManager();
		@SuppressWarnings("unchecked")
		final List<ResourceID> drers = resourceManager
				.listResources(ResourceType.DATA_REQUEST_EXECUTION_RESOURCE);
		// XXX uses first if available - how to determine correct one ?
		final ResourceID drerId = Iterables.getOnlyElement(drers);
		try {
			return (DRER) resourceManager.getResource(drerId);
		} catch (final ResourceUnknownException e) {
			throw new IllegalStateException("drer with id [" + drerId
					+ "] is unknown");
		}
	}
}
