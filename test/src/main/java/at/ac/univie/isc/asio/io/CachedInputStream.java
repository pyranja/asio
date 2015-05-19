/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.io;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Add {@link #cached()} method to {@code ByteArrayInputStream}.
 */
public final class CachedInputStream extends ByteArrayInputStream {
  /**
   * Consume the given {@code InputStream} and cache received data for replay.
   * @param source original stream
   * @return stream, which replays cached data
   * @throws IOException on any error while reading source
   */
  public static CachedInputStream cache(final InputStream source) throws IOException {
    try (final InputStream ignored = source) {
      final byte[] data = ByteStreams.toByteArray(source);
      return new CachedInputStream(data);
    }
  }

  private CachedInputStream(final byte[] buf) {
    super(buf);
  }

  /**
   * @return cached data
   */
  public byte[] cached() {
    return buf;
  }
}
