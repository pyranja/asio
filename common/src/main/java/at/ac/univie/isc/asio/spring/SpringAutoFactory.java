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
package at.ac.univie.isc.asio.spring;

/**
 * Marker interface for auto generated factory classes.
 * If generating a factory with google-auto-factory, it is not possible to annotate with
 * {@link org.springframework.stereotype.Component} for autowiring. To enable detection of the
 * generated factories, include this marker interface in the
 * {@link org.springframework.context.annotation.ComponentScan} directive.
 */
public interface SpringAutoFactory { /* marker */ }
