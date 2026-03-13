package com.example.credvault.controller;

import com.example.credvault.domain.Credential;
import com.example.credvault.dto.CredentialForm;
import com.example.credvault.service.CredentialService;
import com.example.credvault.service.UserAccessService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.util.Set;

@Controller
@RequestMapping("/credentials")
public class CredentialWebController {

    private final CredentialService credentialService;
    private final UserAccessService userAccessService;

    public CredentialWebController(CredentialService credentialService,
                                   UserAccessService userAccessService) {
        this.credentialService = credentialService;
        this.userAccessService = userAccessService;
    }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        model.addAttribute("credentials", credentialService.findAllVisibleForTeams(userTeams));
        return "credentials/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        CredentialForm form = new CredentialForm();
        form.setCreatedBy(authentication.getName());

        Set<String> userTeams = resolveUserTeams(authentication.getName());
        if (userTeams.size() == 1) {
            form.setTeam(userTeams.iterator().next());
        }

        model.addAttribute("form", form);
        model.addAttribute("mode", "create");
        model.addAttribute("teamOptions", userTeams);
        model.addAttribute("mustSelectTeam", userTeams.size() > 1);
        return "credentials/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CredentialForm form,
                         Authentication authentication,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        validateCredentialTeam(form, userTeams, bindingResult);

        if (bindingResult.hasErrors()) {
            form.setCreatedBy(authentication.getName());
            model.addAttribute("mode", "create");
            model.addAttribute("teamOptions", userTeams);
            model.addAttribute("mustSelectTeam", userTeams.size() > 1);
            return "credentials/form";
        }

        credentialService.create(form, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Credencial creada correctamente");
        return "redirect:/credentials";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForTeams(id, userTeams);
            if (!canManageCredential(authentication, credential)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar esta credencial");
                return "redirect:/credentials";
            }
            model.addAttribute("form", credentialService.toForm(credential));
            model.addAttribute("credentialId", id);
            model.addAttribute("mode", "edit");
            model.addAttribute("teamOptions", userTeams);
            model.addAttribute("mustSelectTeam", userTeams.size() > 1);
            return "credentials/form";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/credentials";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CredentialForm form,
                         Authentication authentication,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        validateCredentialTeam(form, userTeams, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("credentialId", id);
            model.addAttribute("teamOptions", userTeams);
            model.addAttribute("mustSelectTeam", userTeams.size() > 1);
            return "credentials/form";
        }

        try {
            Credential credential = credentialService.findByIdVisibleForTeams(id, userTeams);
            if (!canManageCredential(authentication, credential)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar esta credencial");
                return "redirect:/credentials";
            }
            credentialService.update(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Credencial actualizada correctamente");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/credentials";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForTeams(id, userTeams);
            if (!canManageCredential(authentication, credential)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar esta credencial");
                return "redirect:/credentials";
            }
            credentialService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Credencial eliminada");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/credentials";
    }

    @GetMapping("/{id}/history")
    public String history(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        Set<String> userTeams = resolveUserTeams(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForTeams(id, userTeams);
            model.addAttribute("credential", credential);
            model.addAttribute("historyEntries", credentialService.findHistoryByCredentialId(id));
            return "credentials/history";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/credentials";
        }
    }

    private Set<String> resolveUserTeams(String username) {
        return credentialService.normalizeTeams(userAccessService.getTeamsForUser(username));
    }

    private boolean canManageCredential(Authentication authentication, Credential credential) {
        if (isAdmin(authentication)) {
            return true;
        }
        return authentication.getName() != null
                && credential.getCreatedBy() != null
                && authentication.getName().equalsIgnoreCase(credential.getCreatedBy());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private void validateCredentialTeam(CredentialForm form, Set<String> userTeams, BindingResult bindingResult) {
        if (userTeams.isEmpty()) {
            bindingResult.rejectValue("team", "team.required", "Debes pertenecer al menos a un equipo para gestionar credenciales");
            return;
        }

        if (userTeams.size() == 1 && !StringUtils.hasText(form.getTeam())) {
            form.setTeam(userTeams.iterator().next());
            return;
        }

        if (userTeams.size() > 1 && !StringUtils.hasText(form.getTeam())) {
            bindingResult.rejectValue("team", "team.required", "Debes seleccionar un equipo");
            return;
        }

        if (StringUtils.hasText(form.getTeam())) {
            String normalizedTeam = form.getTeam().trim();
            form.setTeam(normalizedTeam);
            if (!userTeams.contains(normalizedTeam)) {
                bindingResult.rejectValue("team", "team.invalid", "Solo puedes asignar credenciales a equipos a los que perteneces");
            }
        }
    }
}
