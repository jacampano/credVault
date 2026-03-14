package io.github.jacampano.credvault.service.credential;

import io.github.jacampano.credvault.dto.CredentialForm;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCredentialHandlerTest {

    private final TokenCredentialHandler handler = new TokenCredentialHandler();

    @Test
    void validateFailsWhenNoExpiryAndDateBothSet() {
        CredentialForm form = new CredentialForm();
        form.setTokenValue("abc");
        form.setTokenNoExpiry(true);
        form.setTokenExpirationDate(LocalDate.now().plusDays(3));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        handler.validate(form, bindingResult);

        assertThat(bindingResult.hasFieldErrors("tokenExpirationDate")).isTrue();
    }

    @Test
    void validateFailsWhenDateIsNotFuture() {
        CredentialForm form = new CredentialForm();
        form.setTokenValue("abc");
        form.setTokenNoExpiry(false);
        form.setTokenExpirationDate(LocalDate.now());
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        handler.validate(form, bindingResult);

        assertThat(bindingResult.hasFieldErrors("tokenExpirationDate")).isTrue();
    }
}
