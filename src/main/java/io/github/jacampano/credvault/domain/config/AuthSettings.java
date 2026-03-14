package io.github.jacampano.credvault.domain.config;

import io.github.jacampano.credvault.security.AuthMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_settings")
public class AuthSettings {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthMode mode = AuthMode.local;

    @Column(length = 200)
    private String oauthClientId;

    @Column(length = 500)
    private String oauthClientSecret;

    @Column(length = 1000)
    private String oauthAuthorizationUri;

    @Column(length = 1000)
    private String oauthTokenUri;

    @Column(length = 1000)
    private String oauthUserInfoUri;

    @Column(length = 120)
    private String oauthUserNameAttribute;

    @Column(length = 500)
    private String oauthScopes;

    @Column(length = 1000)
    private String oauthRedirectUri;

    @Column(length = 1000)
    private String oauthAdminUsers;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthClientSecret() {
        return oauthClientSecret;
    }

    public void setOauthClientSecret(String oauthClientSecret) {
        this.oauthClientSecret = oauthClientSecret;
    }

    public String getOauthAuthorizationUri() {
        return oauthAuthorizationUri;
    }

    public void setOauthAuthorizationUri(String oauthAuthorizationUri) {
        this.oauthAuthorizationUri = oauthAuthorizationUri;
    }

    public String getOauthTokenUri() {
        return oauthTokenUri;
    }

    public void setOauthTokenUri(String oauthTokenUri) {
        this.oauthTokenUri = oauthTokenUri;
    }

    public String getOauthUserInfoUri() {
        return oauthUserInfoUri;
    }

    public void setOauthUserInfoUri(String oauthUserInfoUri) {
        this.oauthUserInfoUri = oauthUserInfoUri;
    }

    public String getOauthUserNameAttribute() {
        return oauthUserNameAttribute;
    }

    public void setOauthUserNameAttribute(String oauthUserNameAttribute) {
        this.oauthUserNameAttribute = oauthUserNameAttribute;
    }

    public String getOauthScopes() {
        return oauthScopes;
    }

    public void setOauthScopes(String oauthScopes) {
        this.oauthScopes = oauthScopes;
    }

    public String getOauthRedirectUri() {
        return oauthRedirectUri;
    }

    public void setOauthRedirectUri(String oauthRedirectUri) {
        this.oauthRedirectUri = oauthRedirectUri;
    }

    public String getOauthAdminUsers() {
        return oauthAdminUsers;
    }

    public void setOauthAdminUsers(String oauthAdminUsers) {
        this.oauthAdminUsers = oauthAdminUsers;
    }
}
