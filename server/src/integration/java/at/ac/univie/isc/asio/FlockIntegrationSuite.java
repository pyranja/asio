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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationDatabase;
import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.spring.ApplicationRunner;
import at.ac.univie.isc.asio.sql.Database;

import com.google.common.io.ByteSource;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.http.MediaType;

import java.net.URI;

@RunWith(Suite.class)
@Suite.SuiteClasses(AllFeatures.class)
public class FlockIntegrationSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Asio.class);

  @BeforeClass
  public static void start() {
    // required for container feature test cases
    IntegrationDatabase.defaultCatalog().h2InMemory()
            .execute(Classpath.read("sql/database.integration.sql"));

    final String[] args = new String[] {
        "--asio.metadata-repository=" + IntegrationTest.atos.address()
    };
    application.profile("flock-test").run(args);

    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + application.getPort() + "/"))
        .auth(AuthMechanism.uri().overrideCredentialDelegationHeader(HttpHeaders.AUTHORIZATION))
        .rootCredentials("root", "change")
        .timeoutInSeconds(10)
        .defaults().schema("public").role(Role.NONE.name());

    IntegrationTest.deploy("public", ByteSource.wrap(Payload.encodeUtf8("{\"identifier\":\"urn:asio:dataset:integration\"}")), MediaType.APPLICATION_JSON_VALUE);
    IntegrationTest.warmup();
  }
}
