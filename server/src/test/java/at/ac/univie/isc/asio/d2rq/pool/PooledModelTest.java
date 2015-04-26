package at.ac.univie.isc.asio.d2rq.pool;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.mockito.Mockito;
import stormpot.Slot;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PooledModelTest {
  private final Slot slot = Mockito.mock(Slot.class);
  private final Model model = ModelFactory.createDefaultModel();
  private final PooledModel subject = new PooledModel(slot, model);
  private final Query query = new Query();

  @Test
  public void should_release_pool_slot() throws Exception {
    subject.release();
    verify(slot).release(subject);
  }

  @Test
  public void should_create__QueryExecution__from_given_query() throws Exception {
    final Query query = new Query();
    final QueryExecution execution = subject.execution(query);
    assertThat(execution.getQuery(), equalTo(query));
  }

  @Test
  public void should_create__QueryExecution__from_wrapped_model() throws Exception {
    final Query query = new Query();
    final QueryExecution execution = subject.execution(query);
    assertThat(execution.getDataset().getDefaultModel(), equalTo(model));
  }

  @Test
  public void should_release_pool_slot_when_closing_the_execution() throws Exception {
    final QueryExecution execution = subject.execution(query);
    execution.close();
    verify(slot).release(subject);
  }

  @Test
  public void should_prevent_multiple_releases_from_proxy() throws Exception {
    final QueryExecution execution = subject.execution(query);
    execution.close();
    execution.close();
    verify(slot, times(1)).release(subject);
  }
}
