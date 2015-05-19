/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.tool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrency utility, that manages a boolean state (active/inactive) and guarded by a lock,
 * providing atomic operations, guaranteed to run only if active.
 */
public final class StatefulMonitor {
  /**
   * Create an instance, that will wait up to the given amount of time when acquiring locks.
   *
   * @param timeout maximal time to wait for a contended lock
   * @return initialized monitor
   */
  public static StatefulMonitor withMaximalWaitingTime(final Timeout timeout) {
    // 0 means 'do not wait at all'
    final long timeoutAsMilliseconds = timeout.getAs(TimeUnit.MILLISECONDS, 0L);
    return new StatefulMonitor(timeoutAsMilliseconds);
  }

  /**
   * Thrown if the monitor is not active, when an atomic operation is executed or state is checked.
   */
  public static final class IllegalMonitorState extends IllegalStateException {
    IllegalMonitorState() {
      super("monitor is not active");
    }
  }


  /**
   * A {@code Callable} that has no return value. The {@link #run()} is similar to {@link Runnable},
   * but may throw checked exceptions.
   */
  public static abstract class Action implements Callable<Void> {
    /**
     * Implement this to define the action.
     */
    public abstract void run() throws Exception;

    /**
     * Execute {@link #run()} and always return {@code null} on success.
     */
    @Override
    public final Void call() throws Exception {
      run();
      return null;
    }
  }

  /**
   * An action for {@link #activate(Callable)} or {@link #disable(Callable)} that does nothing and
   * returns {@code null} on completion.
   */
  public static Callable<Void> noop() {
    return new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        return null;
      }
    };
  }

  /**
   * state where the monitor will execute operations
   */
  static final boolean ACTIVE = true;
  /**
   * state where the monitor will reject operations
   */
  static final boolean INACTIVE = false;

  private final long maximalWaitingTimeInMilliseconds;
  private final ReentrantLock monitor = new ReentrantLock();

  @GuardedBy("monitor")
  private volatile boolean state = false;

  private StatefulMonitor(final long timeout) {
    maximalWaitingTimeInMilliseconds = timeout;
  }

  /**
   * True if this monitor is currently active.
   */
  public boolean isActive() {
    return state;
  }

  /**
   * Checks the monitors current state and fails fast if it is not active.
   *
   * @throws IllegalMonitorState if {@code this} is not active
   */
  public void ensureActive() throws IllegalMonitorState {
    ensureStateIs(ACTIVE);
  }

  /**
   * Transition the monitor to {@code active} state by {@link #atomic(Callable) atomically}
   * executing the supplied action. The monitor will not become active, if the action throws any
   * error.
   *
   * @param action callback to be executed to activate the monitor
   */
  public void activate(final Callable<Void> action) {
    runLockedExpectingState(INACTIVE, new Action() {
      @Override
      public void run() throws Exception {
        action.call();
        state = ACTIVE;
      }
    });
  }

  /**
   * Transition the monitor to {@code inactive} state by {@link #atomic(Callable) atomically}
   * executing the supplied action. The monitor will become inactive, even if the action throws
   * any error.
   *
   * @param action callback to execute to disable the monitor
   */
  public void disable(final Callable<Void> action) {
    runLockedExpectingState(ACTIVE, new Action() {
      @Override
      public void run() throws Exception {
        state = INACTIVE;
        action.call();
      }
    });
  }

  /**
   * Execute the given callback while holding the monitor's lock. The action is not executed if the
   * monitor is not active.
   *
   * @param action   the callback that should be executed atomically
   * @param <RESULT> type of return value of the callable
   * @return the value returned by the callback
   * @throws RuntimeException            unchecked exceptions are propagated as is from the callback
   * @throws IllegalMonitorState         if the component is not active
   * @throws UncheckedExecutionException if the callback throws a checked exception
   * @throws UncheckedTimeoutException   if the lock cannot be acquired in the maximal allowed time
   */
  public <RESULT> RESULT atomic(final Callable<RESULT> action) {
    return runLockedExpectingState(ACTIVE, action);
  }

  // === internals - visible for testing ===========================================================

  /**
   * The internal monitor lock. Only visible for testing purposes.
   */
  @VisibleForTesting
  ReentrantLock monitor() {
    return monitor;
  }

  /**
   * Acquire the monitor lock, ensure the monitor state is the expected, then run the given action
   * and return its result.
   */
  @VisibleForTesting
  /* private */ <RESULT> RESULT runLockedExpectingState(final boolean expected,
                                                        final Callable<RESULT> action) {
    tryAcquireMonitorLock();
    try {
      ensureStateIs(expected);
      return action.call();
    } catch (final Exception failure) {
      Throwables.propagateIfPossible(failure);
      throw new UncheckedExecutionException(failure);
    } finally {
      monitor.unlock();
    }
  }

  private void ensureStateIs(final boolean expected) {
    if (state != expected) { throw new IllegalMonitorState(); }
  }

  private void tryAcquireMonitorLock() {
    try {
      if (!monitor.tryLock(maximalWaitingTimeInMilliseconds, TimeUnit.MILLISECONDS)) {
        throw new UncheckedTimeoutException("timed out while acquiring monitor lock");
      }
    } catch (InterruptedException e) {
      throw new UncheckedTimeoutException("interrupted while acquiring monitor lock", e);
    }
  }
}
