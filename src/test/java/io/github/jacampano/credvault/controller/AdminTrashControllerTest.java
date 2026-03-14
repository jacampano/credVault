package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.service.CredentialService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTrashControllerTest {

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private AdminTrashController adminTrashController;

    @Test
    void listLoadsDeletedCredentials() {
        when(credentialService.findAllDeleted()).thenReturn(List.of(new Credential()));
        Model model = new ExtendedModelMap();

        String view = adminTrashController.list(model);

        assertThat(view).isEqualTo("admin/trash/list");
        assertThat(model.getAttribute("deletedCredentials")).isNotNull();
    }

    @Test
    void restoreRedirectsWithMessage() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminTrashController.restore(4L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/trash");
        verify(credentialService).restore(4L);
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Credencial recuperada correctamente");
    }

    @Test
    void deletePermanentlyRedirectsWithErrorWhenMissing() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new EntityNotFoundException("not found")).when(credentialService).deletePermanently(10L);

        String view = adminTrashController.deletePermanently(10L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/trash");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }
}
