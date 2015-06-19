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

import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.integration.IntegrationDatabase;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.TransientPath;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.tool.Beans;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

/**
 * Start with fixed port
 */
public class Runner {
  /**
   * active profiles
   */
  static final List<String> profiles = Lists.newArrayList("dev", "ajp");

  public static void main(final String[] args) throws IOException {
    final Database database = IntegrationDatabase.defaultCatalog()
        .auto();
    //        .h2InMemory();

    database.execute(Classpath.read("sql/database.integration.sql"));
    database.execute(Classpath.read("sql/gui.integration.sql"));
    profiles.add(database.getType());

    FakeAtosService.attachTo(HttpServer.create("atos-fake").enableLogging()).start(8401);

    try (final TransientPath keystore = TransientPath.file(Classpath.toArray("keystore.integration")).init();
         final TransientPath workingDirectory = TransientPath.folder().init()) {

      workingDirectory.add(
          FileSystemConfigStore.STORE_FOLDER.resolve("public##config"),
          Classpath.load("config.integration.ttl").read());

      final String[] arguments = Beans.extend(args
          , "--server.ssl.key-store=" + keystore.path()
          , "--asio.home=" + workingDirectory.path()
      );

      Asio.application()
          .profiles(profiles.toArray(new String[profiles.size()]))
          .logStartupInfo(true)
          .run(arguments);

      System.out.println("  ===  running...  ===  ");
      System.in.read();
    }
  }
}
