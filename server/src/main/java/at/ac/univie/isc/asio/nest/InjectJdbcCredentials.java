package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.security.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

@Brood
@Order(Ordered.LOWEST_PRECEDENCE) // must be ordered after OverrideJdbcConfig
@ConditionalOnProperty(AsioFeatures.MULTI_TENANCY)
final class InjectJdbcCredentials implements Configurer, OnClose {

  private final MysqlUserRepository repository;

  @Autowired
  public InjectJdbcCredentials(final MysqlUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public String toString() {
    return "InjectJdbcCredentials{}";
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Jdbc jdbc = input.getJdbc();
    final Identity credentials = repository.createUserFor(jdbc.getSchema());
    jdbc.setUsername(credentials.getName()).setPassword(credentials.getSecret());
    return input;
  }

  @Override
  public void cleanUp(final NestConfig spec) throws RuntimeException {
    repository.dropUserOf(spec.getJdbc().getSchema());
  }
}
