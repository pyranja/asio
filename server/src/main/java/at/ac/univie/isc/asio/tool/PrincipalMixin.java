package at.ac.univie.isc.asio.tool;

import com.fasterxml.jackson.annotation.JsonValue;

import java.security.Principal;

/** Serialize principals to their name, suppressing other attributes (e.g. secrets) */
public interface PrincipalMixin extends Principal {
  @JsonValue
  @Override
  String getName();
}
