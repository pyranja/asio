package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.security.Identity;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

import java.sql.SQLException;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;

/**
 * Manage MySql users, segregated by schema.
 */
public final class MysqlUserRepository {
  /** only allows login through socket file from same machine */
  static final String USER_HOST = "localhost";

  private final DSLContext create;

  public MysqlUserRepository(final DataSource dataSource) {
    create = DSL.using(dataSource, SQLDialect.MYSQL);
  }

  /**
   * Create a user, which has access to the given database only.
   *
   * @param schema name of mysql database
   * @return access credentials
   */
  public Identity createUserFor(final String schema) {
    final Identity user = translateToCredentials(schema);
    create.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON {0}.* TO {1}@{2} IDENTIFIED BY {3}",
        name(schema), name(user.getName()), name(USER_HOST), inline(user.getToken()));
    return user;
  }

  /**
   * Delete the user of the given schema. This will only drop users created by this asio.
   *
   * @param schema name of mysql database
   */
  public void dropUserOf(final String schema) {
    final Identity user = translateToCredentials(schema);
    try {
      create.execute("DROP USER {0}@{1}", name(user.getName()), name(USER_HOST));
    } catch (DataAccessException e) {
      if (indicatesFatalFailure(e)) {
        throw e;
      }
    }
  }

  /** mysql emits this error code if a user does not exist in a DROP USER statement */
  static final int ERROR_CODE_DROP_USER_FAILED = 1396;

  private boolean indicatesFatalFailure(final DataAccessException e) {
    if (e.getCause() != null && e.getCause() instanceof SQLException) {
      final SQLException sqlError = (SQLException) e.getCause();
      return sqlError.getErrorCode() != ERROR_CODE_DROP_USER_FAILED;
    } else {
      return true;
    }
  }

  private static final String USERNAME_PREFIX = "_:";
  private static final String PASSWORD_SALT = "asio:";

  /**
   * Derive a unique username and obfuscated password from the given schema name.
   * The username is the base64 encoded SHA-256 hash of the schema name prefixed with {@code '_:'}
   * and truncated to fit the 16 character limit of MySql.
   * The password is the base64 encoded SHA-256 hash of the schema name, salted with the prefix
   * {@code 'asio:'}.
   *
   * @param schema name of a mysql database
   * @return derived, unique credentials
   */
  private Identity translateToCredentials(final String schema) {
    final String hashedSchemaName = BaseEncoding.base64().encode(
        Hashing.sha256().hashString(schema, Charsets.UTF_8).asBytes()
    );
    final String username = USERNAME_PREFIX + hashedSchemaName.substring(0, 14);
    final String password = BaseEncoding.base64().encode(
        Hashing.sha256().hashString(PASSWORD_SALT + schema, Charsets.UTF_8).asBytes()
    );
    return Identity.from(username, password);
  }
}
