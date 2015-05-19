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

import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Predicate;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public final class JenaEngine implements Engine {
  private static final Logger log = LoggerFactory.getLogger(JenaEngine.class);

  public static final String KEY_QUERY = "query";

  public static JenaEngine using(final JenaFactory factory, final boolean allowFederated) {
    return new JenaEngine(factory, allowFederated);
  }

  public static JenaEngine create(final Model model, final Timeout timeout, final boolean allowFederated) {
    return new JenaEngine(new DefaultJenaFactory(model, timeout), allowFederated);
  }

  private final HandlerFactory handlers;
  private final JenaFactory state;
  private final Predicate<Query> detectFederatedQuery;
  private final boolean allowFederated;

  private JenaEngine(final JenaFactory state, final boolean allowFederated) {
    this.state = state;
    this.allowFederated = allowFederated;
    detectFederatedQuery = new IsFederatedQuery();
    handlers = new HandlerFactory();
  }

  @Override
  public Language language() {
    return Language.SPARQL;
  }

  @Override
  public void close() {
    this.state.close();
  }

  @Override
  public SparqlInvocation<?> prepare(final Command command) {
    final Query query = state.parse(command.require(KEY_QUERY));
    log.debug("parsed ARQ query\n{}", query);
    rejectFederatedQueries(query);
    final SparqlInvocation<?> handler = handlers.select(query.getQueryType(), command.acceptable());
    final Principal principal = command.owner().or(Identity.undefined());
    final QueryExecution execution = state.execution(query, principal);
    injectCredentials(execution.getContext(), principal);
    handler.init(execution);
    log.debug("using handler {}", handler);
    return handler;
  }

  private void rejectFederatedQueries(final Query query) {
    if (!allowFederated && detectFederatedQuery.apply(query)) {
      throw new JenaEngine.FederatedQueryLocked();
    }
  }

  public static final Symbol CONTEXT_AUTH_USERNAME =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthUser");
  public static final Symbol CONTEXT_AUTH_PASSWORD =
      Symbol.create("http://jena.hpl.hp.com/Service#queryAuthPwd");

  private void injectCredentials(final Context context, final Principal owner) {
    if (owner instanceof Identity && ((Identity) owner).isDefined()) {
      log.debug("delegating credentials from {}", owner);
      final Identity identity = (Identity) owner;
      context.set(CONTEXT_AUTH_USERNAME, "");
      context.set(CONTEXT_AUTH_PASSWORD, identity.getSecret());
    } else {
      log.debug("skipping credentials delegation - not a valid auth token {}", owner);
    }
  }

  public static final class UnknownQueryType extends InvalidUsage {
    public UnknownQueryType() {
      super("unknown SPARQL query type");
    }
  }

  public static final class FederatedQueryLocked extends InvalidUsage {
    public FederatedQueryLocked() {
      super("execution of federated SPARQL queries is disabled");
    }
  }
}
