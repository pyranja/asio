package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.InvocationBuilderImpl;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Category(FunctionalTest.class)
@Ignore("brittle")
public class ConcurrentSparqlTest extends AcceptanceHarness {
  // TODO : add more sophisticated test cases with mixed / long running queries
  private static final String QUERY = "SELECT (COUNT(*) AS ?total) WHERE { ?s ?p ?o }";

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sparql");
  }

  private static final int THREADS = 20;
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
    final List<Future<Void>> requests = Lists.newArrayList();
    for (int i = 0; i < REQUESTS; i++) {
      final int currentRequestNumber = i;
      final Future<Void> request =
          exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              // cxf specific
              final InvocationBuilderImpl invocation = (InvocationBuilderImpl) client().request(Mime.CSV.type());
              final WebClient localClient = WebClient.fromClient(invocation.getWebClient());
              final Response response = localClient.sync().post(Entity.entity(QUERY, Mime.QUERY_SPARQL.type()));
              response.bufferEntity();
              final String message =
                  "=== "+ currentRequestNumber + ". RESPONSE " + response.getStatus() + " ===\n"
                  + "[" + response.readEntity(String.class) + "]";
              System.out.println(message);
              return null;
            }
          });
      requests.add(request);
    }
    for (Future<Void> request : requests) {
      request.get();
    }
  }
}
