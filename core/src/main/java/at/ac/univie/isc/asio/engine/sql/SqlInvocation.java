package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.tool.Compactor;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import javax.ws.rs.core.MediaType;

abstract class SqlInvocation implements Invocation {
  private final Multimap<String, String> properties;
  private final MediaType format;
  private final Role requiredRole;
  protected final JdbcContext jdbc;
  protected final String sql;

  public SqlInvocation(final JdbcContext jdbc, final String sql, final MediaType format, final Role requiredRole) {
    this.format = format;
    this.sql = sql;
    this.jdbc = jdbc;
    this.requiredRole = requiredRole;
    properties = captureProperties();
  }

  private Multimap<String, String> captureProperties() {
    return ImmutableListMultimap.<String, String>builder()
        .put("command", Compactor.REMOVE_LINE_BREAKS.apply(sql))
        .put("role", requiredRole.toString())
        .put("format", format.toString())
        .put("engine", "jooq")
        .put("dialect", jdbc.dialect())
        .put("timeout", jdbc.timeout().asText())
        .build();
  }

  @Override
  public final Multimap<String, String> properties() {
    return properties;
  }

  @Override
  public final Role requires() {
    return requiredRole;
  }

  @Override
  public final MediaType produces() {
    return format;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("properties", properties)
        .toString();
  }
}
