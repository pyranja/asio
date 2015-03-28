package at.ac.univie.isc.asio.spring;

/**
 * Mark {@link org.springframework.stereotype.Component spring components}, which should be excluded
 * from classpath scanning. An exclusion filter targeting this annotation has to be set on the
 * {@code @ComponentScan} directive.
 */
public @interface ExplicitWiring { /* marker */ }
