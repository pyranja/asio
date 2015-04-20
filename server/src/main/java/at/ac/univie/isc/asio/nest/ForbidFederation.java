package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.Brood;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.Nonnull;

/**
 * If enabled, this configurer disables sparql federation on any deployed container.
 */
@Brood
@ConditionalOnProperty(name = AsioFeatures.ALLOW_FEDERATION, havingValue = "false", matchIfMissing = true)
final class ForbidFederation implements Configurer {
  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    input.getDataset().setFederationEnabled(false);
    return input;
  }

  @Override
  public String toString() {
    return "ForbidFederation{}";
  }
}
