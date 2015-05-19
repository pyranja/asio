/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio;

import com.google.common.io.ByteSource;
import org.springframework.dao.DataAccessException;

import java.net.URI;
import java.util.Map;

/**
 * Provide access to persistent storage for configuration data. Each data item is identified by a
 * {@code name} (describing the contents) and a {@code qualifier} (describing the context). The
 * combination of qualifier and name must be unique for different items.
 */
public interface ConfigStore {

  /**
   * Find all currently stored configuration items with the given identifier as a mapping of
   * {@code qualifier} to content.
   * <p>The mapping is a snapshot of currently stored items - items may be removed and the mapping is
   * <strong>not</strong> updated. Therefore always expect the mapped {@link ByteSource} to throw
   * an exception on access.</p>
   *
   * @param identifier the identifier of the required items, e.g. a special config file suffix
   * @return mapping of qualifier to {@code ByteSource}
   * @throws DataAccessException if the backing storage cannot be accessed
   */
  Map<String, ByteSource> findAllWithIdentifier(final String identifier) throws DataAccessException;

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
