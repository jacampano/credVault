package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.dto.admin.GroupForm;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.service.AdminGroupService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/groups")
public class AdminGroupController {

    private final AdminGroupService adminGroupService;
    private final AuthSettingsService authSettingsService;

    public AdminGroupController(AdminGroupService adminGroupService,
                                AuthSettingsService authSettingsService) {
        this.adminGroupService = adminGroupService;
        this.authSettingsService = authSettingsService;
    }

    @GetMapping
    public String listGroups(Model model) {
        model.addAttribute("groups", adminGroupService.findAll());
        model.addAttribute("oauthManaged", isOauthMode());
        return "admin/groups/list";
    }

    @GetMapping("/new")
    public String newGroupForm(Model model, RedirectAttributes redirectAttributes) {
        if (isOauthMode()) {
            redirectAttributes.addFlashAttribute("error", "En modo OAuth no se permite dar de alta nuevos grupos.");
            return "redirect:/admin/groups";
        }
        model.addAttribute("form", new GroupForm());
        return "admin/groups/create";
    }

    @PostMapping
    public String createGroup(@Valid @ModelAttribute("form") GroupForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (isOauthMode()) {
            redirectAttributes.addFlashAttribute("error", "En modo OAuth no se permite dar de alta nuevos grupos.");
            return "redirect:/admin/groups";
        }
        if (bindingResult.hasErrors()) {
            return "admin/groups/create";
        }
        try {
            adminGroupService.createGroup(form);
            redirectAttributes.addFlashAttribute("message", "Grupo creado correctamente");
            return "redirect:/admin/groups";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            return "admin/groups/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editGroup(@PathVariable Long id,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (isOauthMode()) {
            try {
                Group group = adminGroupService.findById(id);
                if (group.isOauthSynchronized()) {
                    redirectAttributes.addFlashAttribute("error", "Este grupo está sincronizado por OAuth y no se puede editar.");
                    return "redirect:/admin/groups";
                }
                model.addAttribute("form", adminGroupService.toForm(group));
                model.addAttribute("groupId", id);
                return "admin/groups/edit";
            } catch (EntityNotFoundException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/admin/groups";
            }
        }
        try {
            Group group = adminGroupService.findById(id);
            model.addAttribute("form", adminGroupService.toForm(group));
            model.addAttribute("groupId", id);
            return "admin/groups/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/groups";
        }
    }

    @PostMapping("/{id}")
    public String updateGroup(@PathVariable Long id,
                             @Valid @ModelAttribute("form") GroupForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (isOauthMode()) {
            try {
                Group group = adminGroupService.findById(id);
                if (group.isOauthSynchronized()) {
                    redirectAttributes.addFlashAttribute("error", "Este grupo está sincronizado por OAuth y no se puede editar.");
                    return "redirect:/admin/groups";
                }
            } catch (EntityNotFoundException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/admin/groups";
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("groupId", id);
            return "admin/groups/edit";
        }
        try {
            adminGroupService.updateGroup(id, form);
            redirectAttributes.addFlashAttribute("message", "Grupo actualizado correctamente");
            return "redirect:/admin/groups";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("groupId", id);
            return "admin/groups/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/groups";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        if (isOauthMode()) {
            try {
                Group group = adminGroupService.findById(id);
                if (group.isOauthSynchronized()) {
                    redirectAttributes.addFlashAttribute("error", "Este grupo está sincronizado por OAuth y no se puede eliminar.");
                    return "redirect:/admin/groups";
                }
            } catch (EntityNotFoundException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/admin/groups";
            }
        }
        try {
            adminGroupService.deleteGroup(id);
            redirectAttributes.addFlashAttribute("message", "Grupo eliminado");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/groups";
    }

    private boolean isOauthMode() {
        return authSettingsService.loadEffectiveSettings().mode() == AuthMode.oauth;
    }
}
