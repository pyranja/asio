package at.ac.univie.isc.asio.acceptance;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import at.ac.univie.isc.asio.tool.JaxrsClientProvider;
import at.ac.univie.isc.asio.tool.ResponseMonitor;


public abstract class AcceptanceHarness {

  public static final URI SERVER_ADDRESS = URI.create("http://localhost:8080/asio/");
  public static final URI READ_ACCESS = SERVER_ADDRESS.resolve("read/");
  public static final URI FULL_ACCESS = SERVER_ADDRESS.resolve("full/");

  protected static final MediaType CSV = MediaType.valueOf("text/csv").withCharset(
      Charsets.UTF_8.name());
  protected static final MediaType XML = MediaType.APPLICATION_XML_TYPE.withCharset(Charsets.UTF_8
      .name());

  protected static final String PARAM_QUERY = "query";
  protected static final String PARAM_UPDATE = "update";

  protected WebClient client;
  protected Response response;

  private JaxrsClientProvider provider;

  @Rule
  public TestRule clientProvider() {
    JSONProvider jsonSerializer = new JSONProvider();
    jsonSerializer.setNamespaceMap(
        ImmutableMap.of("http://isc.univie.ac.at/2014/asio/metadata", "asio"));
    provider = new JaxrsClientProvider(getTargetUrl()).with(jsonSerializer);
    final ResponseMonitor monitor = new ResponseMonitor(provider);
    return RuleChain.outerRule(provider).around(monitor);
  }

  @Before
  public void injectClient() {
    client = provider.get();
  }

  protected abstract URI getTargetUrl();
}
