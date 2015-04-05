package at.ac.univie.isc.asio.metadata;

import com.google.common.testing.ClassSanityTester;
import org.junit.Test;

public class LinkTest {
  @Test
  public void ensure_sane() throws Exception {
    new ClassSanityTester().forAllPublicStaticMethods(Link.class).testEquals();
  }
}
