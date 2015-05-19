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
package at.ac.univie.isc.asio.matcher;

import com.google.common.collect.Table;
import com.google.common.net.MediaType;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.sun.net.httpserver.HttpExchange;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openjena.riot.Lang;

import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;

public final class RestAssuredMatchers {

  // sql

  public static Matcher<String> sqlCsvEqualTo(final Table<Integer, String, String> expected) {
    return new SqlResultMatcher.Csv(expected);
  }

  public static Matcher<String> sqlWebrowsetEqualTo(final Table<Integer, String, String> expected) {
    return new SqlResultMatcher.Webrowset(expected);
  }

  // jena

  public static Matcher<String> rdfXmlEqualTo(final Model expected) {
    return new RdfModelMatcher(expected, Lang.RDFXML);
  }

  public static Matcher<String> rdfJsonEqualTo(final Model expected) {
    return new RdfModelMatcher(expected, Lang.RDFJSON);
  }

  public static Matcher<String> sparqlXmlEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_XML);
  }

  public static Matcher<String> sparqlJsonEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_JSON);
  }

  public static Matcher<String> sparqlCsvEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_CSV);
  }

  // web

  /**
   * handles 'type/subtype+extension' format
   */
  public static Matcher<String> compatibleTo(final String rawExpectedType) {
    return new MimeMatcher(MediaType.parse(rawExpectedType.toLowerCase(Locale.ENGLISH)));
  }

  // mock http server

  public static Matcher<HttpExchange> basicAuthUsername(final String username) {
    return new ExpectCredentials(equalTo(username), Matchers.any(String.class));
  }

  public static Matcher<HttpExchange> basicAuthPassword(final String password) {
    return new ExpectCredentials(Matchers.any(String.class), equalTo(password));
  }

  private RestAssuredMatchers() { /* factory class */ }
}
