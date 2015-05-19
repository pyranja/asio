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

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Copy all data written to the wrapped {@code OutputStream} into an in-memory buffer.
 */
public final class TeeOutputStream extends FilterOutputStream {
  /**
   * Wrap the given stream and capture all written data.
   * @param out original stream
   * @return wrapped stream
   */
  public static TeeOutputStream wrap(final OutputStream out) {
    return new TeeOutputStream(out);
  }

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private TeeOutputStream(final OutputStream out) {
    super(out);
  }

  @Override
  public void write(final int b) throws IOException {
    buffer.write(b);
    out.write(b);
  }

  @Override
  public void write(@Nonnull final byte[] b) throws IOException {
    buffer.write(b);
    out.write(b);
  }

  @Override
  public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
    buffer.write(b, off, len);
    out.write(b, off, len);
  }

  public byte[] captured() {
    return buffer.toByteArray();
  }
}
