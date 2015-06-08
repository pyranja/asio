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

import com.google.common.base.Optional;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.OWL2;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class D2rqToolsTest {

  public static class FindResourceOfType {
    private final Model model = ModelFactory.createDefaultModel();

    @Test
    public void should_find_single_present_resource() throws Exception {
      final Resource expected = model.createResource("urn:test:resource", OWL2.Thing);
      final Optional<Resource> resource =
          D2rqTools.findSingleOfType(model, OWL2.Thing);
      assertThat(resource.get(), equalTo(expected));
    }

    @Test
    public void should_not_fail_if_no_resource_present() throws Exception {
      final Optional<Resource> resource =
          D2rqTools.findSingleOfType(model, OWL2.Thing);
      assertThat(resource.isPresent(), equalTo(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_fail_if_multiple_resources_find() throws Exception {
      model.createResource("urn:test:one", OWL2.Thing);
      model.createResource("urn:test:two", OWL2.Thing);
      D2rqTools.findSingleOfType(model, OWL2.Thing);
    }
  }

  public static class FindEmbeddedBaseUri {
    private final Model model = ModelFactory.createDefaultModel();

    @Test
    public void should_find_base_uri_if_present() throws Exception {
      final Resource expected = model.createResource("urn:test:base");
      model.createResource("urn:test:server", D2RConfig.Server)
          .addProperty(D2RConfig.baseURI, expected);
      assertThat(D2rqTools.findEmbeddedBaseUri(model).get(), equalTo("urn:test:base"));
    }

    @Test
    public void should_yield_absent_if_property_not_found() throws Exception {
      model.createResource("urn:test:server", D2RConfig.Server);
      assertThat(D2rqTools.findEmbeddedBaseUri(model).isPresent(), equalTo(false));
    }

    @Test
    public void should_yield_absent_if_server_not_present() throws Exception {
      assertThat(D2rqTools.findEmbeddedBaseUri(model).isPresent(), equalTo(false));
    }
  }

  public static class CompileMapping {

    private final ConnectedDB conn = Mockito.mock(ConnectedDB.class);
    private Mapping mapping = new Mapping();

    @Before
    public void prepareMocks() {
      mapping.addDatabase(new Database(ResourceFactory.createResource()));
      when(conn.getJdbcURL()).thenReturn("jdbc:asio:test");
    }

    @Test
    public void should_compile_to_a_model() throws Exception {
      final Model compiled = D2rqTools.compile(mapping, conn);
      assertThat(compiled, not(nullValue()));
    }

    @Test
    public void should_use_given_connection() throws Exception {
      D2rqTools.compile(mapping, conn);
      verify(conn).connection();
    }

    @Test
    public void should_set_limit_and_fetch_size_on_connection() throws Exception {
      final Database database = new Database(ResourceFactory.createResource());
      database.setResultSizeLimit(12);
      database.setFetchSize(33);
      mapping = new Mapping();
      mapping.addDatabase(database);
      D2rqTools.compile(mapping, conn);
      verify(conn).setLimit(12);
      verify(conn).setFetchSize(33);
    }

    @Test
    public void should_set_extended_prefixes() throws Exception {
      final Model compiled = D2rqTools.compile(mapping, conn);
      final Set<Map.Entry<String, String>> actual = compiled.getNsPrefixMap().entrySet();
      final Set<Map.Entry<String, String>> defaults =
          PrefixMapping.Extended.getNsPrefixMap().entrySet();
      assertThat(defaults, everyItem(isIn(actual)));
    }

    @Test
    public void should_hold_given_connection_in_model() throws Exception {
      final Model compiled = D2rqTools.compile(mapping, conn);
      assertThat(compiled.getGraph(), instanceOf(GraphD2RQ.class));
      final Collection<ConnectedDB> connections = new ArrayList<>();
      for (Database database : ((GraphD2RQ) compiled.getGraph()).getMapping().databases()) {
        connections.add(database.connectedDB());
      }
      assertThat(connections, contains(conn));
    }

    @Test(expected = InvalidD2rqConfig.class)
    public void should_fail_on_multiple_databases() throws Exception {
      mapping.addDatabase(new Database(ResourceFactory.createResource()));
      D2rqTools.compile(mapping, conn);
    }
  }

}
