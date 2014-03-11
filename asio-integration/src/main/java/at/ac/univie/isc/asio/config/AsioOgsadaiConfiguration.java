package at.ac.univie.isc.asio.config;

import java.util.List;

import javax.annotation.PostConstruct;

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
import at.ac.univie.isc.asio.ogsadai.DaiExceptionTranslator;
import at.ac.univie.isc.asio.ogsadai.OgsadaiAdapter;
import at.ac.univie.isc.asio.ogsadai.OgsadaiEngine;
import at.ac.univie.isc.asio.ogsadai.OgsadaiJdbcDeployer;
import at.ac.univie.isc.asio.ogsadai.WorkflowComposer;
import at.ac.univie.isc.asio.ogsadai.workflow.SqlComposer;

import com.google.common.collect.Iterables;

/**
 * Setup asio connected to an in-process OGSADAI instance.
 * 
 * @author Chris Borckholder
 */
@Configuration
public class AsioOgsadaiConfiguration {

  @Autowired
  private Environment env;

  @Autowired
  private DatasourceSpec datasource;

  private static final ID ROUTER_ID = new ID("uk.org.ogsadai.MONITORING_FRAMEWORK");
  private static final String DEFAULT_JDBC_TEMPLATE = "uk.org.ogsadai.JDBC_RESOURCE_TEMPLATE";

  @PostConstruct
  public void setupJdbcResource() {
    final String resourceName = env.getRequiredProperty("asio.ogsadai.resource");
    final ResourceID resource = new ResourceID(resourceName, "");
    deployer().deploy(resource, datasource);
  }

  @Bean
  public OGSADAIContext ogsadaiContext() {
    return OGSADAIContext.getInstance();
  }

  @Bean
  public DatasetEngine ogsadaiEngine() {
    return new OgsadaiEngine(adapter(), composer(), translator());
  }

  @Bean
  public DaiExceptionTranslator translator() {
    return new DaiExceptionTranslator();
  }

  @Bean
  public WorkflowComposer composer() {
    final String resourceName = env.getRequiredProperty("asio.ogsadai.resource");
    final ResourceID resource = new ResourceID(resourceName, "");
    return new SqlComposer(resource);
  }

  @Bean
  public OgsadaiAdapter adapter() {
    final RequestEventRouter router = (RequestEventRouter) ogsadaiContext().get(ROUTER_ID);
    final DRER drer = findDRER(ogsadaiContext());
    return new OgsadaiAdapter(drer, router);
  }

  @Bean
  public OgsadaiJdbcDeployer deployer() {
    final ResourceManager manager = ogsadaiContext().getResourceManager();
    final String templateName = env.getProperty("asio.ogsadai.template", DEFAULT_JDBC_TEMPLATE);
    final ResourceID template = new ResourceID(templateName, "");
    return new OgsadaiJdbcDeployer(manager, template);
  }

  // HELPER

  private DRER findDRER(final OGSADAIContext context) {
    final ResourceManager resourceManager = context.getResourceManager();
    @SuppressWarnings("unchecked")
    final List<ResourceID> drers =
        resourceManager.listResources(ResourceType.DATA_REQUEST_EXECUTION_RESOURCE);
    // XXX uses first if available - how to determine correct one ?
    final ResourceID drerId = Iterables.getOnlyElement(drers);
    try {
      return (DRER) resourceManager.getResource(drerId);
    } catch (final ResourceUnknownException e) {
      throw new IllegalStateException("drer with id [" + drerId + "] is unknown");
    }
  }
}
