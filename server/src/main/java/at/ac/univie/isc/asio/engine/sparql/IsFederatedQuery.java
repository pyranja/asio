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
package at.ac.univie.isc.asio.engine.sparql;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementService;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;

import javax.annotation.Nullable;

public final class IsFederatedQuery implements Predicate<Query> {
  @Override
  public boolean apply(@Nullable final Query input) {
    assert input != null : "null query pattern";
    final Element pattern = input.getQueryPattern();
    if (pattern == null) {
      return false; // e.g. DESCRIBE
    } else {
      final DetectServiceElementVisitor detector = new DetectServiceElementVisitor();
      ElementWalker.walk(pattern, detector);
      return detector.isServiceElementFound();
    }
  }

  final static class DetectServiceElementVisitor extends ElementVisitorBase {
    private boolean serviceElementFound = false;

    public boolean isServiceElementFound() {
      return serviceElementFound;
    }

    @Override
    public void visit(final ElementService el) {
      serviceElementFound = true;
    }
  }
}
