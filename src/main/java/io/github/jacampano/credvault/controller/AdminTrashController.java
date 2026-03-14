package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.service.CredentialService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/trash")
public class AdminTrashController {

    private final CredentialService credentialService;

    public AdminTrashController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("deletedCredentials", credentialService.findAllDeleted());
        return "admin/trash/list";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        try {
            credentialService.restore(id);
            redirectAttributes.addFlashAttribute("message", "Credencial recuperada correctamente");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/trash";
    }

    @PostMapping("/{id}/delete-permanently")
    public String deletePermanently(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            credentialService.deletePermanently(id);
            redirectAttributes.addFlashAttribute("message", "Credencial eliminada definitivamente");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/trash";
    }
}
