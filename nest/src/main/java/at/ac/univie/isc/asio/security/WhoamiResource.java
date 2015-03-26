package at.ac.univie.isc.asio.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Component
@Path("/")
public class WhoamiResource {
  private final SecurityContext security;

  @Autowired
  public WhoamiResource(final SecurityContext security) {
    this.security = security;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public AuthInfo getAuthInfo() {
    final Authentication authentication = security.getAuthentication();
    final Identity identity = authentication.getCredentials() instanceof Identity
        ? (Identity) authentication.getCredentials()
        : Identity.undefined();
    return AuthInfo.from(authentication.getName(), identity, authentication.getAuthorities());
  }
}
