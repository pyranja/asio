package at.ac.univie.isc.asio.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provide authentication and authorization information to a client.
 */
@Component
@Path("/whoami")
public class WhoamiResource {
  // TODO : move to CatalogResource ?
  private final SecurityContext security;

  @Autowired
  public WhoamiResource(final SecurityContext security) {
    this.security = security;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public AuthInfo getClientInfo() {
    final Authentication authentication = security.getAuthentication();
    final Role role = Role.valueOf(authentication.getName());
    final Identity identity = authentication.getCredentials() instanceof Identity
        ? (Identity) authentication.getCredentials()
        : Identity.undefined();
    return AuthInfo.from(identity, role);
  }
}
