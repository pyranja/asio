package at.ac.univie.isc.asio.flock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.net.URI;

@ConfigurationProperties("flock")
public class FlockSettings {
  /**
   * Global identifier of the flock service. (default = 'asio:///flock/')
   */
  @NotNull
  public URI identifier;

  @Override
  public String toString() {
    return "FlockSettings{" +
        "identifier=" + identifier +
        '}';
  }

  public URI getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final URI identifier) {
    this.identifier = identifier;
  }
}
