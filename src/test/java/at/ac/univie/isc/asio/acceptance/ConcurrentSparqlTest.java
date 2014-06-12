package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.sql.CsvToTable;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by pyranja on 10/03/14.
 */
@Category(FunctionalTest.class)
public class ConcurrentSparqlTest extends AcceptanceHarness {
  // TODO : add more sophisticated test cases with mixed / long running queries

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sparql");
  }

  private static final int THREADS = 10;
  private static final int REQUESTS = 100;

  private ListeningExecutorService exec;

  @Rule
  public Timeout timeout = new Timeout(20_000);

  @Before
  public void setUp() throws Exception {
    exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(THREADS));
  }

  @After
  public void tearDown() throws Exception {
    exec.shutdownNow();
  }

  @Test
  public void should_execute_concurrent_interleaved_requests_correctly() throws Exception {
    List<ListenableFuture<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < REQUESTS; i++) {
      WebClient client = provider.createUnmanaged();
      tasks.add(exec.submit(new SparqlTask(client)));
    }
    ListenableFuture<List<Void>> completed = Futures.allAsList(tasks);
    completed.get();
  }

  private static class SparqlTask implements Callable<Void> {
    private static final String QUERY = "SELECT (COUNT(*) AS ?total) WHERE { ?s ?p ?o }";

    private final WebClient client;

    private SparqlTask(WebClient client) {
      this.client = client;
    }

    @Override
    public Void call() throws Exception {
      client.accept(CSV).query(PARAM_QUERY, QUERY);
      Response response = client.get();
      final Table<Integer, String, String> result =
          CsvToTable.fromStream((InputStream) response.getEntity());
      assertThat(result.size(), is(1));
      assertThat(result.get(0, "total"), is("1412"));
      return null;
    }
  }
}
