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

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class SelectInvocation extends SparqlInvocation<ResultSet> {

  private final OutputFormatter serializer;

  public SelectInvocation(final OutputFormatter serializer) {
    this(serializer, MediaType.WILDCARD_TYPE);
  }

  public SelectInvocation(final OutputFormatter serializer, final MediaType format) {
    super(format);
    this.serializer = serializer;
  }

  @Override
  protected ResultSet doInvoke(final QueryExecution execution) {
    final ResultSet result = execution.execSelect();
    result.hasNext(); // force fail-fast on backing store
    return result;
  }

  @Override
  protected void doSerialize(final OutputStream sink, final ResultSet data) {
    serializer.format(sink, data);
  }
}
