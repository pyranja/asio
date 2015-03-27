package at.ac.univie.isc.asio;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * Capture events from a guava {@code EventBus}. <strong>ALL</strong> events posted to the provided
 * {@link #bus() EventBus} are captured. The generic type signals which events are expected to be
 * caught for convenient checking.
 *
 * @param <EVENT> type of events that are expected
 */
public final class CaptureEvents<EVENT> {
  /**
   * Create a capturer, with convenience methods for given type of events.
   *
   * @param type   class of expected events
   * @param <TYPE> type of events as given by {@code type} parameter
   * @return event capturer for given type
   */
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

  /**
   * The {@code EventBus} that this capturer is subscribed to. It is a fully functional event bus.
   *
   * @return test event bus
   */
  public EventBus bus() {
    return eventBus;
  }

  /**
   * Drop all events captured until now.
   */
  public void reset() {
    captured.clear();
  }

  /**
   * Get all captured events, including events that are not of the expected type.
   */
  public Iterable<Object> all() {
    return captured;
  }

  /**
   * Get all captured events, that are instances of the expected type or its sub classes,
   * in the same order as they were captured.
   */
  public Iterable<EVENT> captured() {
    return Iterables.filter(captured, type);
  }

  /**
   * Get the single captured event of the expected type. Fails fast if more than one event was
   * captured.
   */
  public EVENT single() {
    return Iterables.getOnlyElement(captured());
  }

  /**
   * Get the i-th captured event of the expected type.
   */
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
