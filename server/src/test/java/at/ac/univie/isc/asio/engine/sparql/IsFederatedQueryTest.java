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
package at.ac.univie.isc.asio.engine.sparql;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class IsFederatedQueryTest {

  private Predicate<Query> subject;

  @Before
  public void setUp() throws Exception {
    subject = new IsFederatedQuery();
  }

  @Test
  public void recognizes_pure_federated_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { SERVICE <http://example.com> { ?s ?p ?o }}");
    assertThat(subject.apply(query), is(true));
  }

  @Test
  public void recognizes_pure_non_federated_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { ?s ?p ?o}");
    assertThat(subject.apply(query), is(false));
  }

  @Test
  public void recognizes_nested_service_in_mixed_query() throws Exception {
    final Query query =
        QueryFactory.create("SELECT * WHERE { ?s ?p ?o . SERVICE <http://example.com> { ?s ?p ?o } }");
    assertThat(subject.apply(query), is(true));
  }

  @Test
  public void recognizes_empty_pattern() throws Exception {
    final Query query = QueryFactory.create("SELECT * WHERE {}");
    assertThat(subject.apply(query), is(false));
  }

  @Test
  public void query_without_pattern_is_ignored() throws Exception {
    final Query query = QueryFactory.create("DESCRIBE <http://example.com>");
    assertThat(subject.apply(query), is(false));
  }
}
