package com.example.credvault.controller;

import com.example.credvault.domain.auth.AppUser;
import com.example.credvault.dto.admin.UserAdminForm;
import com.example.credvault.security.AuthMode;
import com.example.credvault.security.AuthSettingsService;
import com.example.credvault.service.AdminTeamService;
import com.example.credvault.service.AdminUserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.util.Set;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final Set<String> ALLOWED_SORTS = Set.of("username", "firstName", "lastName", "email", "enabled", "id");

    private final AdminUserService adminUserService;
    private final AdminTeamService adminTeamService;
    private final AuthSettingsService authSettingsService;

    public AdminUserController(AdminUserService adminUserService,
                               AdminTeamService adminTeamService,
                               AuthSettingsService authSettingsService) {
        this.adminUserService = adminUserService;
        this.adminTeamService = adminTeamService;
        this.authSettingsService = authSettingsService;
    }

    @GetMapping
    public String listUsers(@RequestParam(name = "q", required = false) String query,
                            @RequestParam(name = "page", required = false) Integer page,
                            @RequestParam(name = "size", required = false) Integer size,
                            @RequestParam(name = "sort", required = false) String sort,
                            @RequestParam(name = "dir", required = false) String dir,
                            Model model) {
        int currentPage = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0 || size > 100) ? 10 : size;
        String sortBy = StringUtils.hasText(sort) && ALLOWED_SORTS.contains(sort) ? sort : "username";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.by(direction, sortBy));
        Page<AppUser> usersPage = adminUserService.listUsers(query, pageable);

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("q", query == null ? "" : query);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("sort", sortBy);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("reverseDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("localMode", isLocalAuthMode());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isLocalAuthMode()) {
            redirectAttributes.addFlashAttribute("error", "Solo se permite crear usuarios en modo local.");
            return "redirect:/admin/users";
        }

        UserAdminForm form = new UserAdminForm();
        form.setEnabled(true);
        form.setAppUserRole(true);
        model.addAttribute("form", form);
        model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
        return "admin/users/create";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("form") UserAdminForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!isLocalAuthMode()) {
            redirectAttributes.addFlashAttribute("error", "Solo se permite crear usuarios en modo local.");
            return "redirect:/admin/users";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
            return "admin/users/create";
        }

        try {
            adminUserService.createUser(form);
            redirectAttributes.addFlashAttribute("message", "Usuario creado correctamente");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
            return "admin/users/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            AppUser user = adminUserService.findById(id);
            model.addAttribute("form", adminUserService.toForm(user));
            model.addAttribute("userId", id);
            model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
            return "admin/users/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("form") UserAdminForm form,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
            return "admin/users/edit";
        }

        try {
            adminUserService.updateUser(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Usuario actualizado correctamente");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("availableTeams", adminTeamService.findAllTeamNames());
            return "admin/users/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            adminUserService.toggleUserStatus(id, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Estado de usuario actualizado");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            adminUserService.deleteUser(id, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Usuario eliminado");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private boolean isLocalAuthMode() {
        return authSettingsService.loadEffectiveSettings().mode() == AuthMode.local;
    }
}
