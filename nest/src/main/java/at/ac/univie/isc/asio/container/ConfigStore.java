package at.ac.univie.isc.asio.container;

import com.google.common.io.ByteSource;
import org.springframework.dao.DataAccessException;

import java.net.URI;

/**
 * Provide access to persistent storage for configuration data. Each data item is identified by a
 * {@code name} (describing the contents) and a {@code qualifier} (describing the context). The
 * combination of qualifier and name must be unique for different items.
 */
public interface ConfigStore {
  /**
   * Persist the given binary content as a configuration item with given name and qualifier. If an
   * item with the same identifier already exists, it will be overwritten.
   * Depending on the persistence mechanism, the returned {@code URI} reference may be used to
   * locate the saved configuration item.
   *
   * @param qualifier  context of configuration item
   * @param identifier label of configuration item
   * @param content    binary configuration data
   * @return reference to the saved item, e.g. a file path
   * @throws org.springframework.dao.DataAccessException on any error during saving
   */
  URI save(String qualifier, String identifier, ByteSource content) throws DataAccessException;

  /**
   * Remove all persisted configuration items for the given context.
   *
   * @param qualifier context that should be cleared
   * @throws org.springframework.dao.DataAccessException on any error during deletion
   */
  void clear(String qualifier) throws DataAccessException;
}
