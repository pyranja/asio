package at.ac.univie.isc.asio;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * Capture events from a guava {@code EventBus}.
 *
 * @param <EVENT>
 */
public final class CaptureEvents<EVENT> {
  public static <TYPE> CaptureEvents<TYPE> create(final Class<TYPE> type) {
    return new CaptureEvents<>(type);
  }

  private final EventBus eventBus;
  private final Class<EVENT> type;
  private final List<Object> captured;

  private CaptureEvents(final Class<EVENT> type) {
    this.type = type;
    this.eventBus = new EventBus("test");
    this.captured = Lists.newArrayList();
    eventBus.register(new CaptureSubscriber());
  }

  public EventBus bus() {
    return eventBus;
  }

  public Iterable<Object> all() {
    return captured;
  }

  public Iterable<EVENT> captured() {
    return Iterables.filter(captured, type);
  }

  public EVENT single() {
    return Iterables.getOnlyElement(captured());
  }

  public EVENT get(final int index) {
    assert index < captured.size();
    return Iterators.get(captured().iterator(), index);
  }

  private class CaptureSubscriber {
    @Subscribe
    public void capture(final Object event) {
      captured.add(event);
    }
  }
}
