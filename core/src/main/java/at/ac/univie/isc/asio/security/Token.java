package at.ac.univie.isc.asio.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.security.Principal;

import static java.util.Objects.requireNonNull;

/**
 * An entity and associated credentials for delegation.
 */
@Immutable
public final class Token implements Principal {
    /**
     * represents the {@code null} token
     */
    static final Token UNDEFINED = new Token("", "");
    @Deprecated
    public static final Token ANONYMOUS = Token.undefined();

    /**
     * Create a token using the given username and password.
     *
     * @param username optional name of the user
     * @param token    required api key
     * @return token object representing the user credentials
     */
    @Nonnull
    public static Token from(@Nullable final String username, @Nonnull final String token) {
        if (username == null || username.isEmpty()) {
            return new Token("", token);
        } else {
            return new Token(username, token);
        }
    }

    /**
     * Create a dummy token, that is not associated with any user or password, e.g. if there is no
     * access control.
     *
     * @return the null token
     */
    @Nonnull
    public static Token undefined() {
        return UNDEFINED;
    }

    private final String name;
    private final String token;

    private Token(@Nonnull final String name, @Nonnull final String token) {
        super();
        this.name = requireNonNull(name, "missing username");
        this.token = requireNonNull(token, "missing token/password");
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * @return the user's api key for delegated auth
     */
    @Nonnull
    public String getToken() {
        return token;
    }

    /**
     * Check if this represents actual credentials.
     *
     * @return true if this is a valid representation.
     */
    public boolean isDefined() {
        return this != UNDEFINED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Token{");
        if (isDefined()) {
            sb.append("name='").append(name).append('\'');
        } else {
            sb.append("undefined");
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final Token that = (Token) other;
        return name.equals(that.name) && token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + token.hashCode();
    }
}
