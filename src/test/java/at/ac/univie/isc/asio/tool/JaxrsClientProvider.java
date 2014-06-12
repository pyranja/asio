package at.ac.univie.isc.asio.tool;

import com.google.common.base.Supplier;
import com.google.common.io.ByteSource;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.rules.ExternalResource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * Sets up a cxf WebClient before each test and disposes it after the test. If a test fails, the
 * contents of the last response received are logged.
 * 
 * @author Chris Borckholder
 */
public final class JaxrsClientProvider extends ExternalResource implements Supplier<WebClient> {

  private final URI baseAddress;
  private final List<Object> providers;
  private WebClient client;

  private boolean secured = false;
  private ByteSource keystoreFile;
  private String password;

  public JaxrsClientProvider(final URI baseAddress) {
    super();
    this.baseAddress = requireNonNull(baseAddress);
    this.providers = new CopyOnWriteArrayList<>();
  }

  public JaxrsClientProvider withKeystore(ByteSource keystore, String password) {
    this.secured = true;
    this.keystoreFile = requireNonNull(keystore);
    this.password = requireNonNull(password);
    return this;
  }

  @Override
  public WebClient get() {
    assert client != null : "JAX-RS client not initialized";
    return client;
  }

  /**
   * @return an unmanaged client instance
   */
  public WebClient createUnmanaged() {
    return createAndInitClient();
  }

  public JaxrsClientProvider with(Object provider) {
    providers.add(provider);
    return this;
  }

  @Override
  protected void before() throws Throwable {
    client = createAndInitClient();
  }

  @Override
  protected void after() {
    client.close();
  }

  private WebClient createAndInitClient() {
    final JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
    factory.setAddress(baseAddress.toString());
    factory.setProviders(providers);
    final WebClient product = factory.createWebClient();
    if (secured) {
      TLSClientParameters tlsParameters = createTlsParameters();
      WebClient.getConfig(product).getHttpConduit().setTlsClientParameters(tlsParameters);
    }
    return product;
  }

  // taken from http://aruld.info/programming-ssl-for-jetty-based-cxf-services/
  private TLSClientParameters createTlsParameters() {
    try {
      TLSClientParameters tlsClientParameters = new TLSClientParameters();

      KeyStore keystore = KeyStore.getInstance("JKS");
      keystore.load(keystoreFile.openStream(), password.toCharArray());

      TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory
          .getDefaultAlgorithm());
      trustFactory.init(keystore);
      TrustManager[] trustManagers = trustFactory.getTrustManagers();
      tlsClientParameters.setTrustManagers(trustManagers);

      KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory
          .getDefaultAlgorithm());
      keyFactory.init(keystore, password.toCharArray());
      KeyManager[] keyManagers = keyFactory.getKeyManagers();
      tlsClientParameters.setKeyManagers(keyManagers);

      FiltersType filter = new FiltersType();
      filter.getInclude().add(".*_EXPORT_.*");
      filter.getInclude().add(".*_EXPORT1024_.*");
      filter.getInclude().add(".*_WITH_DES_.*");
      filter.getInclude().add(".*_WITH_AES_.*");
      filter.getInclude().add(".*_WITH_NULL_.*");
      filter.getExclude().add(".*_DH_anon_.*");
      tlsClientParameters.setCipherSuitesFilter(filter);
      return tlsClientParameters;
    } catch (Exception e) {
      throw new IllegalStateException("TLS configuration failed", e);
    }
  }
}
