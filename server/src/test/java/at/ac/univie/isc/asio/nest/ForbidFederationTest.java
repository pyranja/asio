package at.ac.univie.isc.asio.nest;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ForbidFederationTest {

  private final ForbidFederation subject = new ForbidFederation();

  @Test
  public void should_keep_federation_disabled() throws Exception {
    final NestConfig config = NestConfig.empty();
    config.getDataset().setFederationEnabled(false);
    assertThat(subject.apply(config).getDataset().isFederationEnabled(), equalTo(false));
  }

  @Test
  public void should_disable_federation_if_enabled() throws Exception {
    final NestConfig config = NestConfig.empty();
    config.getDataset().setFederationEnabled(true);
    assertThat(subject.apply(config).getDataset().isFederationEnabled(), equalTo(false));
  }
}
