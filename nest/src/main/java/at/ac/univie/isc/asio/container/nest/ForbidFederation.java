package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.AsioFeatures;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * If enabled, this configurer disables sparql federation on any deployed container.
 */
@Component
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
