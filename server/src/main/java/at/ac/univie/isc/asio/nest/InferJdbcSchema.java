package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.tool.JdbcTools;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

/**
 * If no jdbc:schema attribute is set explicitly, attempt to infer the default database from the
 * jdbc url. Works only for mysql connection strings.
 */
@Brood
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InferJdbcSchema implements Configurer {

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Jdbc jdbc = input.getJdbc();
    if (jdbc.getSchema() == null) {
      jdbc.setSchema(JdbcTools.inferSchema(jdbc.getUrl()).orNull());
    }
    return input;
  }
}
