package io.github.jacampano.credvault.service.credential;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.dto.CredentialForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

@Component
public class WebUserPasswordCredentialHandler implements CredentialTypeHandler {

    @Override
    public CredentialType supports() {
        return CredentialType.WEB_USER_PASSWORD;
    }

    @Override
    public void validate(CredentialForm form, BindingResult bindingResult) {
        if (!StringUtils.hasText(form.getWebUsername())) {
            bindingResult.rejectValue("webUsername", "required", "El nombre de usuario es obligatorio");
        }
        if (!StringUtils.hasText(form.getWebPassword())) {
            bindingResult.rejectValue("webPassword", "required", "La password es obligatoria");
        }
    }

    @Override
    public void applyToCredential(CredentialForm form, Credential credential) {
        credential.setWebUsername(trimToNull(form.getWebUsername()));
        credential.setWebPassword(trimToNull(form.getWebPassword()));
        credential.setWebUrl(trimToNull(form.getWebUrl()));

        credential.setTokenValue(null);
        credential.setTokenUrl(null);
        credential.setTokenExpirationDate(null);
        credential.setTokenNoExpiry(false);
    }

    @Override
    public void fillForm(Credential credential, CredentialForm form) {
        form.setWebUsername(credential.getWebUsername());
        form.setWebPassword(credential.getWebPassword());
        form.setWebUrl(credential.getWebUrl());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
