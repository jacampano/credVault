package io.github.jacampano.credvault.service.credential;

import io.github.jacampano.credvault.dto.CredentialForm;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class WebUserPasswordCredentialHandlerTest {

    private final WebUserPasswordCredentialHandler handler = new WebUserPasswordCredentialHandler();

    @Test
    void validateRequiresUsernameAndPassword() {
        CredentialForm form = new CredentialForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        handler.validate(form, bindingResult);

        assertThat(bindingResult.hasFieldErrors("webUsername")).isTrue();
        assertThat(bindingResult.hasFieldErrors("webPassword")).isTrue();
    }
}
