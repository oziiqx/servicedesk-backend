package pl.servicedesk.identity.security;

import java.util.Collection;
import java.util.List;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<SimpleGrantedAuthority> authorities;

    private AuthenticatedUser(Long id, String email, String passwordHash, boolean active, List<String> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public static AuthenticatedUser forLogin(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.isActive(),
                user.getRoles().stream().map(Role::authority).toList());
    }

    public static AuthenticatedUser fromToken(Long id, String email, List<String> authorities) {
        return new AuthenticatedUser(id, email, null, true, authorities);
    }

    public Long id() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", email=" + email + "]";
    }
}
