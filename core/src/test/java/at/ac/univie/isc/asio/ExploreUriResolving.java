package at.ac.univie.isc.asio;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ExploreUriResolving {

  @Parameterized.Parameters(name = "{index} : <{0}> + <{1}> => <{2}>")
  public static Iterable<Object[]> fragments() {
    return Arrays.asList(new Object[][] {
        { "/base", "other", "/other" }
        , { "/base/", "other", "/base/other" }
        , { "/base", "/other", "/other" }
        , { "/base/", "/other", "/other" }
        , { "base", "other", "other" }
        , { "base/", "other", "base/other" }
        , { "base", "/other", "/other" }
        , { "base/", "/other", "/other" }
        , { "base//", "other", "base/other" }
    });
  }

  @Parameterized.Parameter(0)
  public String base;
  @Parameterized.Parameter(1)
  public String other;
  @Parameterized.Parameter(2)
  public String expected;

  @Test
  public void resolve() throws Exception {
    assertThat(URI.create(base).resolve(other), is(URI.create(expected)));
  }
}
