/*
 * #%L
 * asio cli
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
package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Pigeon;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

final class Trace implements Command {
  private static final Logger log = getLogger(Trace.class);

  private final Appendable console;
  private final Pigeon pigeon;

  public Trace(final Appendable sink, final Pigeon pigeon) {
    console = sink;
    this.pigeon = pigeon;
  }

  @Override
  public String toString() {
    return "tail the server event-stream (break with Ctrl-C)";
  }

  @Override
  public int call(final List<String> ignored) throws IOException {
    final EventSource events = pigeon.eventStream();
    final ConsoleListener listener = new ConsoleListener(console);
    events.register(listener);
    try {
      log.info("subscribing to event-trace {}", events);
      events.open();
      listener.stop.await();
    } catch (InterruptedException e) {
      log.info("event-trace interrupted");
    } finally {
      log.info("event-trace stopping");
      events.close(0, TimeUnit.MILLISECONDS);
    }
    return 0;
  }

  private static class ConsoleListener implements EventListener {
    final CountDownLatch stop = new CountDownLatch(1);
    private final Appendable sink;

    private ConsoleListener(final Appendable sink) {
      this.sink = sink;
    }

    @Override
    public void onEvent(final InboundEvent received) {
      try {
        final Map map = received.readData(Map.class);
        sink.append(Objects.toString(map)).append(System.lineSeparator());
      } catch (IOException e) {
        log.error("[ERROR] failed to write event stream - {}", e.getMessage());
        stop.countDown();
      }
    }
  }
}
