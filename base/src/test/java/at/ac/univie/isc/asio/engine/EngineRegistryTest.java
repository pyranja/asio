package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Token;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.schedulers.Schedulers;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EngineRegistryTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private Command.Factory subject;

  @Test
  public void language_not_registered() throws Exception {
    subject = new EngineRegistry(Schedulers.immediate(), Collections.<Engine>emptySet());
    error.expect(Command.Factory.LanguageNotSupported.class);
    final Parameters params = Parameters.builder(Language.valueOf("TEST")).build();
    subject.accept(params, Token.ANONYMOUS);
  }

  @Test
  public void delegates_to_registered_engine() throws Exception {
    final Engine engine = mock(Engine.class);
    when(engine.language()).thenReturn(Language.valueOf("TEST"));
    subject = new EngineRegistry(Schedulers.immediate(), Sets.newHashSet(engine));
    final Parameters params = Parameters.builder(Language.valueOf("TEST")).build();
    subject.accept(params, Token.ANONYMOUS);
    verify(engine).prepare(params, Token.ANONYMOUS);
  }
}
