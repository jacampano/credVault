package io.github.jacampano.credvault.service.credential;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.dto.CredentialForm;
import org.springframework.validation.BindingResult;

public interface CredentialTypeHandler {

    CredentialType supports();

    void validate(CredentialForm form, BindingResult bindingResult);

    void applyToCredential(CredentialForm form, Credential credential);

    void fillForm(Credential credential, CredentialForm form);
}
