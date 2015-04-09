package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.junit.Interactions;
import org.glassfish.jersey.media.sse.InboundEvent;
import rx.Observer;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventReport implements Interactions.Report, Observer<InboundEvent> {
  private final Queue<InboundEvent> received = new ConcurrentLinkedQueue<>();
  private Throwable error;

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    for (InboundEvent event : received) {
      sink.append(event.toString()).append(System.lineSeparator());
    }
    if (error != null) {
      sink.append("Terminated in error : ").append(error.toString());
    }
    error = null;
    received.clear();
    return sink;
  }

  @Override
  public void onCompleted() {}

  @Override
  public void onError(final Throwable e) {
    error = e;
  }

  @Override
  public void onNext(final InboundEvent event) {
    received.add(event);
  }

  @Override
  public String toString() {
    return "event-stream";
  }
}
