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
package at.ac.univie.isc.asio.d2rq.pool;

import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.d2rq.D2rqTools;
import at.ac.univie.isc.asio.database.Jdbc;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import stormpot.Slot;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class D2rqModelAllocatorTest {

  private final Slot slot = Mockito.mock(Slot.class);
  private D2rqModelAllocator subject;

  @Before
  public void createAllocator() {
    // minimal valid d2rq configuration
    final Model d2rqModel = ModelFactory.createDefaultModel();
    d2rqModel.createResource(D2RQ.Database);
    final D2rqConfigModel d2rq = D2rqConfigModel.wrap(d2rqModel);
    final Jdbc jdbc = new Jdbc().setUrl("jdbc:h2:mem:");  // mock jdbc connection
    subject = new D2rqModelAllocator(d2rq, jdbc);
  }

  @Test
  public void should_replace_a_model_with_broken_connection() throws Exception {
    final PooledModel pooled = subject.allocate(slot);
    sabotageConnectionOn(pooled);
    final PooledModel refreshed = subject.reallocate(slot, pooled);
    assertThat(refreshed.getModel(), not(sameInstance(pooled.getModel())));
  }

  @Test
  public void should_replace_closed_model() throws Exception {
    final PooledModel pooled = subject.allocate(slot);
    pooled.getModel().close();
    final PooledModel refreshed = subject.reallocate(slot, pooled);
    assertThat(refreshed.getModel(), not(sameInstance(pooled.getModel())));
  }

  @Test
  public void should_close_model_with_broken_connection() throws Exception {
    final PooledModel pooled = subject.allocate(slot);
    sabotageConnectionOn(pooled);
    subject.reallocate(slot, pooled);
    assertThat(pooled.getModel().isClosed(), is(true));
  }

  @Test
  public void should_close_d2rq_model_on_deallocation() throws Exception {
    final Model model = Mockito.mock(Model.class);
    final PooledModel pooled = new PooledModel(slot, model);
    subject.deallocate(pooled);
    verify(model).close();
  }

  private void sabotageConnectionOn(final PooledModel pooled) throws SQLException {
    final Connection connection = D2rqTools.unwrapDatabaseConnection(pooled.getModel()).connection();
    connection.close();
    assertThat(connection.isValid(5), is(false));
  }

}
