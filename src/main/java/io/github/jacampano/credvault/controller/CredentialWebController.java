package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.dto.CredentialForm;
import io.github.jacampano.credvault.service.CredentialService;
import io.github.jacampano.credvault.service.UserAccessService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/credentials")
public class CredentialWebController {

    private final CredentialService credentialService;
    private final UserAccessService userAccessService;
    private static final Set<String> ALLOWED_SORTS = Set.of("identifier", "type", "groups", "shared", "updatedAt");

    public CredentialWebController(CredentialService credentialService,
                                   UserAccessService userAccessService) {
        this.credentialService = credentialService;
        this.userAccessService = userAccessService;
    }

    @GetMapping
    public String list(@RequestParam(name = "sort", required = false) String sort,
                       @RequestParam(name = "dir", required = false) String dir,
                       @RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "system", required = false) List<String> systems,
                       @RequestParam(name = "component", required = false) List<String> components,
                       @RequestParam(name = "environment", required = false) List<String> environments,
                       @RequestParam(name = "type", required = false) List<String> types,
                       @RequestParam(name = "group", required = false) List<String> selectedGroups,
                       @RequestParam(name = "shared", required = false) String shared,
                       Model model,
                       Authentication authentication) {
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        String sortBy = StringUtils.hasText(sort) && ALLOWED_SORTS.contains(sort) ? sort : "updatedAt";
        String direction = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        Set<String> selectedSystemFilters = normalizeSelection(systems);
        Set<String> selectedComponentFilters = normalizeSelection(components);
        Set<String> selectedEnvironmentFilters = normalizeSelection(environments);
        Set<String> selectedTypeFilters = normalizeSelection(types).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> selectedGroupFilters = normalizeSelection(selectedGroups);
        String sharedFilter = normalizeSharedFilter(shared);
        String identifierQuery = StringUtils.hasText(q) ? q.trim() : "";
        String identifierQueryLower = identifierQuery.toLowerCase(Locale.ROOT);
        List<Credential> credentials = new ArrayList<>(
                credentialService.findAllVisibleForUser(authentication.getName(), userGroups)
        );
        credentials = credentials.stream()
                .filter(c -> !StringUtils.hasText(identifierQueryLower)
                        || safeLower(c.getIdentifier()).contains(identifierQueryLower))
                .filter(c -> selectedSystemFilters.isEmpty()
                        || (c.getInformationComponent() != null
                        && c.getInformationComponent().getInformationSystem() != null
                        && selectedSystemFilters.contains(c.getInformationComponent().getInformationSystem().getIdentifier())))
                .filter(c -> selectedComponentFilters.isEmpty()
                        || (c.getInformationComponent() != null
                        && selectedComponentFilters.contains(c.getInformationComponent().getIdentifier())))
                .filter(c -> selectedEnvironmentFilters.isEmpty()
                        || (c.getEnvironment() != null
                        && selectedEnvironmentFilters.contains(c.getEnvironment().getIdentifier())))
                .filter(c -> selectedTypeFilters.isEmpty()
                        || (c.getType() != null && selectedTypeFilters.contains(c.getType().name())))
                .filter(c -> selectedGroupFilters.isEmpty()
                        || (!Collections.disjoint(c.getGroups(), selectedGroupFilters)))
                .filter(c -> "all".equals(sharedFilter)
                        || ("yes".equals(sharedFilter) && c.isShared())
                        || ("no".equals(sharedFilter) && !c.isShared()))
                .collect(Collectors.toCollection(ArrayList::new));
        credentials.sort(buildComparator(sortBy, direction));

        List<String> systemOptions = credentialService.findAvailableComponents().stream()
                .map(component -> component.getInformationSystem().getIdentifier())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> componentOptions = credentialService.findAvailableComponents().stream()
                .map(component -> component.getIdentifier())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> environmentOptions = credentialService.findAvailableEnvironments().stream()
                .map(environment -> environment.getIdentifier())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        model.addAttribute("credentials", credentials);
        model.addAttribute("sort", sortBy);
        model.addAttribute("dir", direction);
        model.addAttribute("reverseDir", "asc".equals(direction) ? "desc" : "asc");
        model.addAttribute("systemOptions", systemOptions);
        model.addAttribute("componentOptions", componentOptions);
        model.addAttribute("environmentOptions", environmentOptions);
        model.addAttribute("groupOptions", userGroups);
        model.addAttribute("credentialTypes", credentialService.supportedTypes());
        model.addAttribute("selectedSystems", selectedSystemFilters);
        model.addAttribute("selectedComponents", selectedComponentFilters);
        model.addAttribute("selectedEnvironments", selectedEnvironmentFilters);
        model.addAttribute("selectedTypes", selectedTypeFilters);
        model.addAttribute("selectedGroups", selectedGroupFilters);
        model.addAttribute("sharedFilter", sharedFilter);
        model.addAttribute("q", identifierQuery);
        return "credentials/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        CredentialForm form = new CredentialForm();
        form.setCreatedBy(authentication.getName());
        fillCommonModel(model, form, "create", null, resolveUserGroups(authentication.getName()));
        return "credentials/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CredentialForm form,
                         Authentication authentication,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        validateSelectedGroups(form, userGroups, bindingResult);
        credentialService.validateByType(form, bindingResult);

        if (bindingResult.hasErrors()) {
            form.setCreatedBy(authentication.getName());
            fillCommonModel(model, form, "create", null, userGroups);
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
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForUser(id, authentication.getName(), userGroups);
            if (!canManageCredential(authentication, credential)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar esta credencial");
                return "redirect:/credentials";
            }
            fillCommonModel(model, credentialService.toForm(credential), "edit", id, userGroups);
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
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        validateSelectedGroups(form, userGroups, bindingResult);
        credentialService.validateByType(form, bindingResult);

        if (bindingResult.hasErrors()) {
            fillCommonModel(model, form, "edit", id, userGroups);
            return "credentials/form";
        }

        try {
            Credential credential = credentialService.findByIdVisibleForUser(id, authentication.getName(), userGroups);
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
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForUser(id, authentication.getName(), userGroups);
            if (!canManageCredential(authentication, credential)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar esta credencial");
                return "redirect:/credentials";
            }
            credentialService.delete(id, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Credencial enviada a la papelera");
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
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        try {
            Credential credential = credentialService.findByIdVisibleForUser(id, authentication.getName(), userGroups);
            model.addAttribute("credential", credential);
            model.addAttribute("historyEntries", credentialService.findHistoryByCredentialId(id));
            return "credentials/history";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/credentials";
        }
    }

    @PostMapping("/{id}/copy")
    @ResponseBody
    public ResponseEntity<Void> auditCopyAction(@PathVariable Long id,
                                                Authentication authentication) {
        Set<String> userGroups = resolveUserGroups(authentication.getName());
        try {
            credentialService.findByIdVisibleForUser(id, authentication.getName(), userGroups);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private void fillCommonModel(Model model, CredentialForm form, String mode, Long credentialId, Set<String> userGroups) {
        model.addAttribute("form", form);
        model.addAttribute("mode", mode);
        model.addAttribute("credentialId", credentialId);
        model.addAttribute("componentOptions", credentialService.findAvailableComponents());
        model.addAttribute("environmentOptions", credentialService.findAvailableEnvironments());
        model.addAttribute("groupOptions", userGroups);
        model.addAttribute("credentialTypes", credentialService.supportedTypes());
    }

    private Set<String> resolveUserGroups(String username) {
        return credentialService.normalizeGroups(userAccessService.getGroupsForUser(username));
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

    private void validateSelectedGroups(CredentialForm form, Set<String> userGroups, BindingResult bindingResult) {
        Set<String> selectedGroups = form.getSelectedGroups() == null ? Set.of() : new LinkedHashSet<>(form.getSelectedGroups());
        Set<String> normalizedSelected = selectedGroups.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        form.setSelectedGroups(normalizedSelected);
        form.setShared(!normalizedSelected.isEmpty());
        if (normalizedSelected.isEmpty()) {
            return;
        }
        boolean invalidGroup = normalizedSelected.stream().anyMatch(group -> !userGroups.contains(group));
        if (invalidGroup) {
            bindingResult.rejectValue("selectedGroups", "group.invalid", "Solo puedes compartir con grupos a los que perteneces");
        }
    }

    private Comparator<Credential> buildComparator(String sortBy, String direction) {
        Comparator<Credential> comparator = switch (sortBy) {
            case "identifier" -> Comparator.comparing(c -> safeLower(c.getIdentifier()));
            case "type" -> Comparator.comparing(c -> c.getType() == null ? "" : c.getType().name());
            case "groups" -> Comparator.comparing(c -> String.join(",", c.getGroups()));
            case "shared" -> Comparator.comparing(Credential::isShared);
            case "updatedAt" -> Comparator.comparing(Credential::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(Credential::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if ("desc".equals(direction)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private Set<String> normalizeSelection(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeSharedFilter(String shared) {
        if (!StringUtils.hasText(shared)) {
            return "all";
        }
        String normalized = shared.trim().toLowerCase(Locale.ROOT);
        if ("yes".equals(normalized) || "no".equals(normalized)) {
            return normalized;
        }
        return "all";
    }
}
