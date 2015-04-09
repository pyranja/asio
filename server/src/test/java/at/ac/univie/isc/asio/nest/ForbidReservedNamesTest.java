package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public final class ForbidReservedNamesTest {

  @Test
  public void should_keep_config_as_is_if_not_reserved() throws Exception {
    final ForbidReservedNames subject = new ForbidReservedNames(Collections.<Id>emptyList());
    final NestConfig input = NestConfig.empty();
    input.getDataset().setName(Id.valueOf("test"));
    assertThat(subject.apply(input), equalTo(input));
  }

  @Test(expected = ForbidReservedNames.IllegalContainerName.class)
  public void should_reject_config_with_reserved_name() throws Exception {
    final ForbidReservedNames subject =
        new ForbidReservedNames(Arrays.asList(Id.valueOf("reserved")));
    final NestConfig input = NestConfig.empty();
    input.getDataset().setName(Id.valueOf("reserved"));
    subject.apply(input);
  }
}
