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
package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.InvalidUsage;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Thrown if a given d2rq mapping cannot be read.
 */
public final class InvalidD2rqConfig extends InvalidUsage {
  InvalidD2rqConfig(final RDFNode offender, final String reason) {
    super("<" + offender + "> is illegal - " + reason);
  }
}
