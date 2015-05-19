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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class DescribeInvocation extends SparqlInvocation<Model> {

  private final RDFWriter writer;

  public DescribeInvocation(final RDFWriter writer, final MediaType format) {
    super(format);
    this.writer = writer;
  }

  @Override
  protected Model doInvoke(final QueryExecution execution) {
    return execution.execDescribe();
  }

  @Override
  protected void doSerialize(final OutputStream sink, final Model data) {
    writer.write(data, sink, "");
  }

}
