package com.example.credvault.dto.admin;

import com.example.credvault.security.AuthMode;
import jakarta.validation.constraints.NotNull;

public class AuthenticationSettingsForm {

    @NotNull
    private AuthMode mode = AuthMode.local;

    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthAuthorizationUri;
    private String oauthTokenUri;
    private String oauthUserInfoUri;
    private String oauthUserNameAttribute;
    private String oauthScopes;
    private String oauthRedirectUri;
    private String oauthAdminUsers;

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
