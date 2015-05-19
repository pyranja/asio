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
package at.ac.univie.isc.asio.spring;

/**
 * Generic contract for proxy-able holder objects. This allows to inject a proxied holder object
 * into singletons, that need to set the delegate for their current scope.
 * By using this interface, the backing holder implementation may be final and spring can
 * nonetheless use JDK dynamic proxies instead of cglib proxies.
 *
 * @param <TYPE> type of held object
 */
public interface Holder<TYPE> {
  /**
   * Inject an instance as delegate of this holder.
   *
   * @param delegate the instance to hold
   */
  void set(TYPE delegate);
}
