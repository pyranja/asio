package at.ac.univie.isc.asio.acceptance;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import at.ac.univie.isc.asio.tool.JaxrsClientProvider;
import at.ac.univie.isc.asio.tool.ResponseMonitor;

import com.google.common.base.Charsets;


public abstract class AcceptanceHarness {

  public static final URI SERVER_ADDRESS = URI.create("http://localhost:8080/asio/");

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
    provider = new JaxrsClientProvider(getTargetUrl());
    final ResponseMonitor monitor = new ResponseMonitor(provider);
    return RuleChain.outerRule(provider).around(monitor);
  }

  @Before
  public void injectClient() {
    client = provider.get();
  }

  protected abstract URI getTargetUrl();
}
