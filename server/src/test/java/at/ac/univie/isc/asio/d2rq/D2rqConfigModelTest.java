package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class D2rqConfigModelTest {
  private final Model model = ModelFactory.createDefaultModel();
  private final D2rqConfigModel subject = D2rqConfigModel.wrap(model);

  @Test
  public void should_find_base_uri() throws Exception {
    model.createResource("urn:test:server", D2RConfig.Server)
        .addProperty(D2RConfig.baseURI, model.createResource("urn:asio:default"));
    assertThat(subject.getBaseUri(), equalTo(URI.create("urn:asio:default")));
  }

  @Test
  public void should_use_default_if_base_uri_missing() throws Exception {
    assertThat(subject.getBaseUri(), equalTo(URI.create(D2rqTools.DEFAULT_BASE)));
  }

  @Test(expected = InvalidD2rqConfig.class)
  public void should_fail_if_server_is_anonymous() throws Exception {
    model.createResource(D2RConfig.Server);
    subject.getIdentifier();
  }

  @Test
  public void should_use_server_resource_as_identifier() throws Exception {
    model.createResource("urn:test:server", D2RConfig.Server);
    assertThat(subject.getIdentifier(), equalTo(URI.create("urn:test:server")));
  }

  @Test
  public void should_parse_into_mapping() throws Exception {
    assertThat(subject.getMapping(), not(nullValue()));
  }

  @Test
  public void should_enable_all_optimizations_on_mapping() throws Exception {
    assertThat(subject.getMapping().configuration().getUseAllOptimizations(), equalTo(true));
  }

  @Test
  public void should_yield_undefined_if_timeout_property_missing() throws Exception {
    model.createResource("urn:test:server", D2RConfig.Server);
    assertThat(subject.getTimeout(), equalTo(Timeout.undefined()));
  }

  @Test(expected = InvalidD2rqConfig.class)
  public void should_fail_if_timeout_value_illegal() throws Exception {
    model.createResource("urn:test:server", D2RConfig.Server)
        .addLiteral(D2RConfig.sparqlTimeout, "not-a-number");
    subject.getTimeout();
  }

  @Test
  public void should_find_timeout() throws Exception {
    model.createResource("urn:test:server", D2RConfig.Server)
        .addLiteral(D2RConfig.sparqlTimeout, 100);
    assertThat(subject.getTimeout(), equalTo(Timeout.from(100, TimeUnit.SECONDS)));
  }
  
  @Test
  public void should_disable_federation_if_service_description_missing() throws Exception {
    assertThat(subject.isFederationEnabled(), equalTo(false));
  }

  @Test
  public void should_disable_federation_if_federation_feature_missing() throws Exception {
    model.createResource("urn:asio:service", SparqlServiceDescription.Service);
    assertThat(subject.isFederationEnabled(), equalTo(false));
  }

  @Test
  public void should_enable_federation_if_service_feature_present() throws Exception {
    model.createResource("urn:asio:service", SparqlServiceDescription.Service)
        .addProperty(SparqlServiceDescription.feature, SparqlServiceDescription.BasicFederatedQuery);
    assertThat(subject.isFederationEnabled(), equalTo(true));
  }
}
