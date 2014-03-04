package at.ac.univie.isc.asio.security;

import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.Action;

public interface Authorized {

  Set<Action> permitted();
}
