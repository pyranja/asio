package at.ac.univie.isc.asio.ogsadai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.authorization.DistinguishedNameProvider;
import uk.org.ogsadai.authorization.Login;
import uk.org.ogsadai.authorization.LoginProviderException;
import uk.org.ogsadai.authorization.ManageableLoginProvider;
import uk.org.ogsadai.authorization.NullSecurityContext;
import uk.org.ogsadai.authorization.SecurityContext;
import uk.org.ogsadai.authorization.file.SimpleFileLoginProvider;
import uk.org.ogsadai.resource.ResourceCreationException;
import uk.org.ogsadai.resource.ResourceFactory;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.ResourceIDAlreadyAssignedException;
import uk.org.ogsadai.resource.ResourceManager;
import uk.org.ogsadai.resource.ResourceTypeException;
import uk.org.ogsadai.resource.ResourceUnknownException;
import uk.org.ogsadai.resource.SimpleResourceFactory;
import uk.org.ogsadai.resource.dataresource.jdbc.JDBCDataResource;
import uk.org.ogsadai.resource.dataresource.jdbc.JDBCDataResourceState;
import at.ac.univie.isc.asio.config.DatasourceSpec;

/**
 * Setup an OGSADAI {@link JDBCDataResource} from a given {@link DatasourceSpec}.
 * 
 * @author Chris Borckholder
 */
public class OgsadaiJdbcDeployer {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(OgsadaiJdbcDeployer.class);

  private final ResourceFactory factory;
  private final ResourceID template;

  public OgsadaiJdbcDeployer(final ResourceManager manager, final ResourceID template) {
    super();
    factory = new SimpleResourceFactory(new NullSecurityContext(), manager);
    this.template = template;
  }

  public void deploy(final ResourceID alias, final DatasourceSpec spec) {
    log.debug("deploying {} from spec {}", alias, spec);
    final JDBCDataResource it = constructNewResource();
    final JDBCDataResourceState state = it.getJDBCDataResourceState();
    injectSettings(state, alias, spec);
    injectCredentials(state, alias, spec);
    publish(it, alias);
    log.info("deployed {} from spec {}", alias, spec);
  }

  private JDBCDataResource constructNewResource() {
    try {
      return (JDBCDataResource) factory.createDataResource(template);
    } catch (ResourceCreationException | ResourceTypeException | ResourceUnknownException
        | ClassCastException e) {
      throw new IllegalStateException("failed to create a JDBC data resource", e);
    }
  }

  private void injectSettings(final JDBCDataResourceState state, final ResourceID alias,
      final DatasourceSpec spec) {
    state.getDataResourceState().setResourceID(alias);
    state.getDataResourceState().setTransient(true);
    state.setDatabaseURL(spec.getJdbcUrl());
    state.setDriverClass(spec.getJdbcDriver());
  }

  private void injectCredentials(final JDBCDataResourceState state, final ResourceID alias,
      final DatasourceSpec spec) throws LoginProviderException {
    final Login credentials = new Login(spec.getUsername(), spec.getPassword());
    final ManageableLoginProvider loginator = (ManageableLoginProvider) state.getLoginProvider();
    loginator.permitLogin(alias, WildcardSecurityContext.INSTANCE, credentials);
  }

  private void publish(final JDBCDataResource it, final ResourceID alias)
      throws IllegalStateException {
    try {
      factory.addResource(alias, it);
    } catch (final ResourceIDAlreadyAssignedException e) {
      throw new IllegalStateException("given resource alias " + alias + " is already assigned");
    }
  }

  /**
   * Hacked for use with flawed {@link SimpleFileLoginProvider}. This context will always return the
   * wildcard DN <code>*</code>.
   */
  private static enum WildcardSecurityContext implements SecurityContext, DistinguishedNameProvider {
    INSTANCE;

    @Override
    public String getDN() {
      return "*";
    }

  }
}
