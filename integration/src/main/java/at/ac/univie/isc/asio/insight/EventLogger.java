package at.ac.univie.isc.asio.insight;

import org.glassfish.jersey.media.sse.InboundEvent;
import rx.Observer;

public class EventLogger implements Observer<InboundEvent> {
  @Override
  public void onCompleted() {}

  @Override
  public void onError(final Throwable e) {
    System.err.println("Terminated in error : " + e);
  }

  @Override
  public void onNext(final InboundEvent inboundEvent) {
    System.out.println(inboundEvent);
  }
}
