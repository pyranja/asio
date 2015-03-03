package at.ac.univie.isc.asio.metadata;

import com.google.common.testing.ClassSanityTester;
import org.junit.Test;
import org.threeten.bp.*;

import java.util.Arrays;

public class DatasetDescriptionTest {
  @Test
  public void verify_class_constraints() throws Exception {
    new ClassSanityTester()
        .setSampleInstances(ZonedDateTime.class, Arrays.asList(ZonedDateTime.now(), ZonedDateTime.of(LocalDate.of(1984, Month.NOVEMBER, 28), LocalTime.of(8, 20), ZoneOffset.UTC)))
        .testEquals(DatasetDescription.class);
  }
}
