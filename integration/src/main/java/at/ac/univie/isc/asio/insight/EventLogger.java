/*
 * #%L
 * asio integration
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
