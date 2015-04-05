package at.ac.univie.isc.asio.spring;

/**
 * Generic contract for proxy-able holder objects. This allows to inject a proxied holder object
 * into singletons, that need to set the delegate for their current scope.
 * By using this interface, the backing holder implementation may be final and spring can
 * nonetheless use JDK dynamic proxies instead of cglib proxies.
 *
 * @param <TYPE> type of held object
 */
public interface Holder<TYPE> {
  /**
   * Inject an instance as delegate of this holder.
   *
   * @param delegate the instance to hold
   */
  void set(TYPE delegate);
}
