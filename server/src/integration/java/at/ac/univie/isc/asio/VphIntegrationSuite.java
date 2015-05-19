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
import at.ac.univie.isc.asio.io.TransientPath;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.spring.ApplicationRunner;
import at.ac.univie.isc.asio.sql.Database;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;

/**
 * Run nest integration tests using the URI-based auth mechanism required for VPH deployments.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(AllFeatures.class)
public class VphIntegrationSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Asio.class);
  @ClassRule
  public static TransientPath workDirectory = TransientPath.folder();

  @BeforeClass
  public static void start() throws IOException {

    final Database database = IntegrationDatabase.defaultCatalog()
        .auto()
        .execute(Classpath.read("sql/database.integration.sql"));

    workDirectory.add(
        FileSystemConfigStore.STORE_FOLDER.resolve("public##config"),
        Classpath.load("config.integration.ttl").read()
    );

    final String[] args = new String[] {
        "--asio.home=" + workDirectory.path(),
        "--asio.metadata-repository=" + IntegrationTest.atos.address()
    };
    application.profile("vph-test").profile(database.getType()).run(args);

    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + application.getPort() + "/"))
        .auth(AuthMechanism.uri().overrideCredentialDelegationHeader(HttpHeaders.AUTHORIZATION))
        .rootCredentials("root", "change")
        .database(database)
        .timeoutInSeconds(10)
        .defaults().schema("public").role(Role.NONE.name());

    IntegrationTest.warmup();
  }
}
