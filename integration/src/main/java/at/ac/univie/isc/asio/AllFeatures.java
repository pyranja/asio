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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.SuiteType;
import org.junit.runner.RunWith;

/**
 * Pick up all asio Feature* and Reference* test cases from the {@code at.ac.univie.isc.asio} package,
 * i.e. includes pre packaged from the integration package <strong>and</strong> module-local tests.
 * Either include this in another {@link org.junit.runners.Suite suite}, that handles setup and
 * test configuration or extend this class to create a full suite.
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.IncludeJars(true)
@ClasspathSuite.BaseTypeFilter(IntegrationTest.class)
@ClasspathSuite.ClassnameFilters({"(.*)asio\\.Feature(.+)", "(.*)asio\\.Reference(.+)"})
@ClasspathSuite.SuiteTypes({SuiteType.TEST_CLASSES, SuiteType.RUN_WITH_CLASSES})
public class AllFeatures {}
