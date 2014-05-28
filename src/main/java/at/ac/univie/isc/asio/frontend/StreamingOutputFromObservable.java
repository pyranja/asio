package at.ac.univie.isc.asio.frontend;

import at.ac.univie.isc.asio.DatasetTransportException;
import com.google.common.annotations.VisibleForTesting;
import rx.Observable;
import rx.functions.Action2;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
* @author pyranja
*/
@VisibleForTesting
class StreamingOutputFromObservable implements StreamingOutput {
  private static final Action2<OutputStream, byte[]> STREAM_COLLECTOR =
      new Action2<OutputStream, byte[]>() {
        @Override
        public void call(final OutputStream sink, final byte[] bytes) {
          try {
            sink.write(bytes);
          } catch (IOException e) {
            throw new DatasetTransportException(e);
          }
        }
      };

  public static StreamingOutputFromObservable bridge(final Observable<byte[]> observable) {
    return new StreamingOutputFromObservable(observable);
  }

  private final Observable<byte[]> observable;

  StreamingOutputFromObservable(final Observable<byte[]> observable) {
    this.observable = observable;
  }

  @Override
  public void write(final OutputStream output) throws IOException, WebApplicationException {
    observable.collect(output, STREAM_COLLECTOR).toBlocking().single();
  }
}
