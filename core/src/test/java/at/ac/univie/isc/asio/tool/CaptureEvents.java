package at.ac.univie.isc.asio.tool;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;

public class CaptureEvents {
  public static CaptureEvents create() {
    return new CaptureEvents();
  }
  private final EventBus eventBus;

  private final List<Object> captured;

  private CaptureEvents() {
    this.eventBus = new EventBus("test");
    this.captured = Lists.newArrayList();
    eventBus.register(new CaptureSubscriber());
  }

  public EventBus bus() {
    return eventBus;
  }

  public Iterable<Object> captured() {
    return captured;
  }

  public <EXPECTED> Iterable<EXPECTED> captured(final Class<EXPECTED> expected) {
    return Iterables.filter(captured, expected);
  }

  private class CaptureSubscriber {
    @Subscribe
    public void capture(final Object event) {
      captured.add(event);
    }
  }
}
