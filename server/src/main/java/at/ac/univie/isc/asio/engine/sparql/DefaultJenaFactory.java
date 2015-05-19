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

import at.ac.univie.isc.asio.tool.Closer;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;

import javax.annotation.Nonnull;
import java.security.Principal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class DefaultJenaFactory implements JenaFactory {

  private final Timeout timeout;
  private final Dataset dataset;
  private final Model model;

  public DefaultJenaFactory(final Model model, final Timeout timeout) {
    this.model = model;
    this.dataset = DatasetFactory.create(model);
    this.timeout = timeout;
  }

  @Override
  public Query parse(final String sparql) {
    final Query query = QueryFactory.create();
    query.getPrefixMapping().withDefaultMappings(model);
    QueryFactory.parse(query, sparql, null, Syntax.syntaxARQ);
    return query;
  }

  @Override
  public QueryExecution execution(final Query query, final Principal owner) {
    final QueryExecution execution = QueryExecutionFactory.create(query, dataset);
    execution.setTimeout(timeout.getAs(MILLISECONDS, -1L), MILLISECONDS);
    return execution;
  }

  @Override
  public void close() {
    Closer.quietly(model, new Closer<Model>() {
      @Override
      public void close(@Nonnull final Model it) throws Exception {
        it.close();
      }
    });
  }
}
