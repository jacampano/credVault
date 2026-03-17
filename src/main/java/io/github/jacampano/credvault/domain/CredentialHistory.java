package io.github.jacampano.credvault.domain;

import io.github.jacampano.credvault.crypto.EncryptedStringConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "credential_history")
public class CredentialHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private Credential credential;

    @Column(nullable = false, length = 120)
    private String editedBy;

    @Column(nullable = false)
    private Instant editedAt;

    @Column(name = "identifier", nullable = false, length = 120)
    private String identifier;

    @Column(nullable = false, length = 120)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CredentialType type = CredentialType.WEB_USER_PASSWORD;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credential_history_groups", joinColumns = @JoinColumn(name = "history_id"))
    @Column(name = "group_name", nullable = false, length = 120)
    private Set<String> groups = new HashSet<>();

    @Column(nullable = false)
    private boolean shared;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_username", length = 4096)
    private String webUsername;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_password", length = 4096)
    private String webPassword;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_url", length = 4096)
    private String webUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_token_value", length = 4096)
    private String tokenValue;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_token_url", length = 4096)
    private String tokenUrl;

    @Column(name = "token_expiration_date")
    private LocalDate tokenExpirationDate;

    @Column(name = "token_no_expiry", nullable = false)
    private boolean tokenNoExpiry;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_notes", length = 4096)
    private String notes;

    @PrePersist
    void onCreate() {
        if (this.editedAt == null) {
            this.editedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getWebUsername() {
        return webUsername;
    }

    public void setWebUsername(String webUsername) {
        this.webUsername = webUsername;
    }

    public String getWebPassword() {
        return webPassword;
    }

    public void setWebPassword(String webPassword) {
        this.webPassword = webPassword;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public LocalDate getTokenExpirationDate() {
        return tokenExpirationDate;
    }

    public void setTokenExpirationDate(LocalDate tokenExpirationDate) {
        this.tokenExpirationDate = tokenExpirationDate;
    }

    public boolean isTokenNoExpiry() {
        return tokenNoExpiry;
    }

    public void setTokenNoExpiry(boolean tokenNoExpiry) {
        this.tokenNoExpiry = tokenNoExpiry;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
