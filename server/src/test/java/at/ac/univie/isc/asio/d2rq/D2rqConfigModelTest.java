/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import org.hamcrest.Matchers;
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

  @Test
  public void should_provide_a_copy_of_the_input_model() throws Exception {
    final Model held = subject.getDefinition();
    assertThat(held, not(sameInstance(model)));
    final Resource anon = model.createResource();
    assertThat(held.containsResource(anon), equalTo(false));
  }

  @Test
  public void should_keep_top_level_server_resources() throws Exception {
    final Resource server = model.createResource(D2RConfig.Server);
    final Model held = subject.getDefinition();
    assertThat(held.containsResource(server), equalTo(true));
  }

  @Test
  public void should_hide_database_properties() throws Exception {
    final Resource original = model.createResource(D2RQ.Database);
    original.addProperty(D2RQ.jdbcDSN, "jdbc:mysql:///");
    original.addProperty(D2RQ.username, "username");
    original.addProperty(D2RQ.password, "password");
    final Model held = subject.getDefinition();
    final Resource copied =
        Iterators.getOnlyElement(held.listResourcesWithProperty(RDF.type, D2RQ.Database));
    assertThat(Iterators.size(copied.listProperties()), equalTo(1));
    final Statement typeProperty = Iterators.getOnlyElement(copied.listProperties());
    assertThat(typeProperty.getPredicate(), equalTo(RDF.type));
    assertThat(typeProperty.getObject(), Matchers.<RDFNode>equalTo(D2RQ.Database));
    assertThat(copied, not(equalTo(original)));
  }

  @Test
  public void should_keep_prefixes_in_model_creator_method() throws Exception {
    model.setNsPrefix("my", "urn:test:my-prefix");
    final Model model = subject.getDefinition();
    assertThat(model.getNsPrefixURI("my"), equalTo("urn:test:my-prefix"));
  }
}
