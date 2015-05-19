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
package at.ac.univie.isc.asio.spring;

import com.google.common.io.ByteSource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Adapt a Spring {@code Resource} to the Guava {@code ByteSource} contract.
 */
public final class SpringByteSource extends ByteSource {
  /**
   * Wrap a spring resource as ByteSource.
   * @param delegate spring resource
   * @return wrapping ByteSource
   */
  public static SpringByteSource asByteSource(final Resource delegate) {
    requireNonNull(delegate);
    return new SpringByteSource(delegate);
  }

  private final Resource delegate;

  private SpringByteSource(final Resource delegate) {
    this.delegate = delegate;
  }

  @Override
  public InputStream openStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public String toString() {
    return delegate.getDescription();
  }
}
