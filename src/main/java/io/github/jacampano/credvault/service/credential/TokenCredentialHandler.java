package io.github.jacampano.credvault.service.credential;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.dto.CredentialForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;

@Component
public class TokenCredentialHandler implements CredentialTypeHandler {

    @Override
    public CredentialType supports() {
        return CredentialType.TOKEN;
    }

    @Override
    public void validate(CredentialForm form, BindingResult bindingResult) {
        if (!StringUtils.hasText(form.getTokenValue())) {
            bindingResult.rejectValue("tokenValue", "required", "El token es obligatorio");
        }

        if (form.isTokenNoExpiry() && form.getTokenExpirationDate() != null) {
            bindingResult.rejectValue("tokenExpirationDate", "invalid", "Selecciona fecha de expiración o marca NO CADUCA, pero no ambas");
        }

        if (!form.isTokenNoExpiry() && form.getTokenExpirationDate() == null) {
            bindingResult.rejectValue("tokenExpirationDate", "required", "Debes indicar una fecha de expiración o marcar NO CADUCA");
        }

        if (!form.isTokenNoExpiry() && form.getTokenExpirationDate() != null) {
            LocalDate today = LocalDate.now();
            if (!form.getTokenExpirationDate().isAfter(today)) {
                bindingResult.rejectValue("tokenExpirationDate", "future", "La fecha de expiración debe ser posterior a la fecha actual");
            }
        }
    }

    @Override
    public void applyToCredential(CredentialForm form, Credential credential) {
        credential.setTokenValue(trimToNull(form.getTokenValue()));
        credential.setTokenUrl(trimToNull(form.getTokenUrl()));
        credential.setTokenNoExpiry(form.isTokenNoExpiry());
        credential.setTokenExpirationDate(form.isTokenNoExpiry() ? null : form.getTokenExpirationDate());

        credential.setWebUsername(null);
        credential.setWebPassword(null);
        credential.setWebUrl(null);
    }

    @Override
    public void fillForm(Credential credential, CredentialForm form) {
        form.setTokenValue(credential.getTokenValue());
        form.setTokenUrl(credential.getTokenUrl());
        form.setTokenNoExpiry(credential.isTokenNoExpiry());
        form.setTokenExpirationDate(credential.getTokenExpirationDate());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
