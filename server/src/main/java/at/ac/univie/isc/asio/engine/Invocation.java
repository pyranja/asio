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
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Permission;
import com.google.common.collect.Multimap;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap a two-phase operation on a dataset.
 * <p>
 * An invocation has to be {@link #execute() executed} first,
 * then results can be {@link #write(java.io.OutputStream) written} to a sink exactly once.
 * </p>
 */
public interface Invocation extends AutoCloseable {

  /**
   * @return role required to execute this.
   */
  Permission requires();

  /**
   * @return contextual information
   */
  Multimap<String, String> properties();

  /**
   * Perform the operation and prepare resources for result serialization.
   *
   * @throws java.lang.IllegalStateException        if this has already been {@link #execute() executed}
   */
  void execute();

  /**
   * @return the MIME type of the serialized results.
   */
  MediaType produces();

  /**
   * Write serialized results to the given {@code sink}.
   *
   * @param sink an {@link java.io.OutputStream}
   * @throws java.io.IOException                    if writing to the given {@code sink} fails.
   */
  void write(OutputStream sink) throws IOException;

  /**
   * Interrupt this invocation, if it is active. Concurrent {@link #execute()} or
   * {@link #write(java.io.OutputStream)} may fail due to interruption.
   */
  void cancel();

  /**
   * Free all resources associated with this invocation. If the invocation is {@code active} it may
   * be interrupted. Closing an already closed {@code Invocation} has no effect.
   */
  @Override
  void close();
}
