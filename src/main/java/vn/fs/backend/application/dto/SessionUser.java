package vn.fs.backend.application.dto;

import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

public class SessionUser implements Serializable {
    private Long   userId;
    private String username;
    private String name;
    private String email;
    private String avatar;
    private Set<String> roles;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public static SessionUser from(User u) {
        SessionUser su = new SessionUser();
        su.setUserId(u.getUserId());
        su.setUsername(u.getUsername());
        su.setName(u.getName());
        su.setEmail(u.getEmail());
        su.setAvatar(u.getAvatar());
        if (u.getRoles() != null) {
            su.setRoles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        }
        return su;
    }
}
