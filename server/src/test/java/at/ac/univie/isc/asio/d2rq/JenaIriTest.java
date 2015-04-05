package at.ac.univie.isc.asio.d2rq;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/** explorative tests to understand jena iri behavior */
public class JenaIriTest {
  @Test
  public void should_parse_urn() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("urn:asio:default#");
    assertThat(iri.getScheme(), equalTo("urn"));
    assertThat(iri.getRawPath(), equalTo("asio:default"));
    assertThat(iri.getRawFragment(), equalTo(""));
    assertThat(iri.isRootless(), equalTo(true));
  }

  @Test
  public void should_resolve_relative_uri_against_urn() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("urn:asio:default#");
    final IRI resolved = iri.resolve("it");
    assertThat(resolved.getScheme(), equalTo("urn"));
    assertThat(resolved.getRawPath(), equalTo("it"));
    assertThat(resolved.getRawFragment(), nullValue());
    assertThat(resolved.toASCIIString(), equalTo("urn:it"));
  }

  @Test
  public void should_resolve_relative_uri_starting_with_fragment_against_urn() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("urn:asio:default");
    final IRI resolved = iri.resolve("#it");
    assertThat(resolved.getScheme(), equalTo("urn"));
    assertThat(resolved.getRawPath(), equalTo("asio:default"));
    assertThat(resolved.getRawFragment(), equalTo("it"));
    assertThat(resolved.toASCIIString(), equalTo("urn:asio:default#it"));
  }

  @Test
  public void should_parse_asio_scheme() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("asio:///default/");
    assertThat(iri.getScheme(), equalTo("asio"));
    assertThat(iri.getRawPath(), equalTo("/default/"));
    assertThat(iri.getRawFragment(), nullValue());
    assertThat(iri.isRootless(), equalTo(false));
  }

  @Test
  public void should_resolve_relative_uri_against_asio_scheme() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("asio:///default/");
    final IRI resolved = iri.resolve("it");
    assertThat(resolved.getScheme(), equalTo("asio"));
    assertThat(resolved.getRawPath(), equalTo("/default/it"));
    assertThat(resolved.getRawFragment(), nullValue());
    assertThat(resolved.toASCIIString(), equalTo("asio:///default/it"));
  }

  @Test
  public void should_resolve_relative_uri_starting_with_fragment_against_asio_scheme() throws Exception {
    final IRI iri = IRIFactory.jenaImplementation().create("asio:///default/");
    final IRI resolved = iri.resolve("#it");
    assertThat(resolved.getScheme(), equalTo("asio"));
    assertThat(resolved.getRawPath(), equalTo("/default/"));
    assertThat(resolved.getRawFragment(), equalTo("it"));
    assertThat(resolved.toASCIIString(), equalTo("asio:///default/#it"));
  }
}
