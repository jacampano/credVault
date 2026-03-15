package io.github.jacampano.credvault.dto.admin;

import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.OAuthClientAuthenticationMethod;
import io.github.jacampano.credvault.security.OAuthProvider;
import jakarta.validation.constraints.NotNull;

public class AuthenticationSettingsForm {

    @NotNull
    private AuthMode mode = AuthMode.local;

    @NotNull
    private OAuthProvider oauthProvider = OAuthProvider.generic;

    private String oauthClientId;
    private String oauthClientSecret;
    private OAuthClientAuthenticationMethod oauthClientAuthenticationMethod = OAuthClientAuthenticationMethod.client_secret_post;
    private String oauthGitlabBaseUrl;
    private String oauthAuthorizationUri;
    private String oauthTokenUri;
    private String oauthUserInfoUri;
    private String oauthUserNameAttribute;
    private String oauthScopes;
    private String oauthRedirectUri;
    private String oauthAdminGroups;

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public OAuthProvider getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(OAuthProvider oauthProvider) {
        this.oauthProvider = oauthProvider;
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

    public OAuthClientAuthenticationMethod getOauthClientAuthenticationMethod() {
        return oauthClientAuthenticationMethod;
    }

    public void setOauthClientAuthenticationMethod(OAuthClientAuthenticationMethod oauthClientAuthenticationMethod) {
        this.oauthClientAuthenticationMethod = oauthClientAuthenticationMethod;
    }

    public String getOauthGitlabBaseUrl() {
        return oauthGitlabBaseUrl;
    }

    public void setOauthGitlabBaseUrl(String oauthGitlabBaseUrl) {
        this.oauthGitlabBaseUrl = oauthGitlabBaseUrl;
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

    public String getOauthAdminGroups() {
        return oauthAdminGroups;
    }

    public void setOauthAdminGroups(String oauthAdminGroups) {
        this.oauthAdminGroups = oauthAdminGroups;
    }
}
