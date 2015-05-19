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

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap {@link at.ac.univie.isc.asio.engine.Invocation} results and provide them as stream entity.
 */
public abstract class StreamedResults implements StreamingOutput, AutoCloseable {
  private final Subject<Void, Void> progress = BehaviorSubject.create();
  private final MediaType format;

  protected StreamedResults(final MediaType format) {
    this.format = format;
  }

  /**
   * Called to write the result stream.
   *
   * @param output the OutputStream to write to.
   * @throws java.io.IOException if an IO error is encountered
   */
  protected abstract void doWrite(final OutputStream output) throws IOException;

  /**
   * Write the serialized results to the supplied {@link java.io.OutputStream}.
   *
   * @param output the OutputStream to write to.
   * @throws java.io.IOException                    if an IO error is encountered
   */
  @Override
  public final void write(final OutputStream output) throws IOException {
    try {
      doWrite(output);
      progress.onCompleted();
    } catch (final Throwable cause) {
      progress.onError(cause);
      throw cause;
    }
  }

  /**
   * Release resources associated with the results, for example an open database cursor. Closing
   * <strong>may</strong> discard not yet consumed result data and abort an active serialization.
   *
   * The default is a no-op, implementations should override this method.
   */
  @Override
  public void close() {};

  /**
   * Empty observable, which propagates errors during streaming and completes when all results were
   * streamed.
   *
   * @return observable progress of result stream
   */
  public final Observable<Void> progress() {
    return progress;
  }

  /**
   * @return the MIME type of this serialized result.
   */
  public final MediaType format() {
    return format;
  }
}
