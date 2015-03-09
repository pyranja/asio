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
