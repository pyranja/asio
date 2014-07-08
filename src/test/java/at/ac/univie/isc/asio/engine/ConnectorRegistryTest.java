package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.protocol.Parameters;
import at.ac.univie.isc.asio.security.Token;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

public class ConnectorRegistryTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  @Test
  public void should_delegate_to_found_connector() throws Exception {
    final Connector mock = Mockito.mock(Connector.class);
    final ConnectorRegistry subject = ConnectorRegistry.builder().add(Language.SPARQL, mock).build();
    final Parameters params = Parameters.builder(Language.SPARQL).build();
    subject.createCommand(params, Token.ANONYMOUS);
    verify(mock).createCommand(params, Token.ANONYMOUS);
  }

  @Test
  public void should_fail_if_language_not_available() throws Exception {
    final ConnectorRegistry subject = ConnectorRegistry.builder().build();
    error.expect(Connector.LanguageNotSupported.class);
    subject.createCommand(Parameters.builder(Language.SPARQL).build(), Token.ANONYMOUS);
  }
}
