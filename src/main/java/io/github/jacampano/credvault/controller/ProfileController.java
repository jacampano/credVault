package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.dto.ProfileForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ProfileController {

    private final AuthSettingsService authSettingsService;
    private final AppUserRepository appUserRepository;

    public ProfileController(AuthSettingsService authSettingsService,
                             AppUserRepository appUserRepository) {
        this.authSettingsService = authSettingsService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        AuthMode mode = authSettingsService.loadEffectiveSettings().mode();
        ProfileForm form = (mode == AuthMode.local)
                ? loadLocalProfile(authentication.getName())
                : loadOauthProfile(authentication);

        model.addAttribute("form", form);
        model.addAttribute("editable", mode == AuthMode.local);
        model.addAttribute("mode", mode.name());
        model.addAttribute("oauthGroups", mode == AuthMode.oauth ? extractOauthGroups(authentication) : List.of());
        return "profile/view";
    }

    @PostMapping("/profile")
    public String updateLocalProfile(Authentication authentication,
                                     @Valid @ModelAttribute("form") ProfileForm form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        AuthMode mode = authSettingsService.loadEffectiveSettings().mode();
        if (mode != AuthMode.local) {
            redirectAttributes.addFlashAttribute("error", "En modo OAUTH el perfil se actualiza automáticamente.");
            return "redirect:/profile";
        }

        form.setUsername(authentication.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("editable", true);
            model.addAttribute("mode", mode.name());
            return "profile/view";
        }

        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario local no encontrado"));

        NameParts nameParts = splitFullName(form.getFullName());
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setEmail(trimToNull(form.getEmail()));
        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "Perfil actualizado correctamente");
        return "redirect:/profile";
    }

    private ProfileForm loadLocalProfile(String username) {
        ProfileForm form = new ProfileForm();
        form.setUsername(username);

        appUserRepository.findByUsername(username).ifPresent(user -> {
            form.setFullName(joinFullName(user.getFirstName(), user.getLastName()));
            form.setEmail(user.getEmail());
        });

        return form;
    }

    private ProfileForm loadOauthProfile(Authentication authentication) {
        ProfileForm form = new ProfileForm();

        String username = authentication.getName();
        String fullName = null;
        String email = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            username = firstNonBlank(oidcUser.getPreferredUsername(), oidcUser.getEmail(), oidcUser.getName(), username);
            fullName = firstNonBlank(
                    oidcUser.getFullName(),
                    joinFullName(oidcUser.getGivenName(), oidcUser.getFamilyName()),
                    joinFullName(oidcUser.getClaimAsString("given_name"), oidcUser.getClaimAsString("family_name")),
                    oidcUser.getName()
            );
            email = firstNonBlank(oidcUser.getEmail(), oidcUser.getClaimAsString("email"));
        } else if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();
            username = firstNonBlank(readString(attrs, "username"), readString(attrs, "preferred_username"), readString(attrs, "login"), readString(attrs, "name"), username);
            fullName = firstNonBlank(
                    readString(attrs, "name"),
                    joinFullName(readString(attrs, "given_name"), readString(attrs, "family_name")),
                    joinFullName(readString(attrs, "first_name"), readString(attrs, "last_name"))
            );
            email = firstNonBlank(readString(attrs, "email"));
        }

        form.setUsername(username);
        form.setFullName(fullName);
        form.setEmail(email);
        return form;
    }

    private List<String> extractOauthGroups(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oauth2User)) {
            return List.of();
        }
        Object groups = oauth2User.getAttributes().get("gitlabGroupPaths");
        if (!(groups instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String value && StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private String readString(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String joinFullName(String firstName, String lastName) {
        String first = trimToNull(firstName);
        String last = trimToNull(lastName);
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    private NameParts splitFullName(String fullName) {
        String normalized = trimToNull(fullName);
        if (normalized == null) {
            return new NameParts(null, null);
        }
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace < 0) {
            return new NameParts(normalized, null);
        }
        String first = trimToNull(normalized.substring(0, firstSpace));
        String last = trimToNull(normalized.substring(firstSpace + 1));
        return new NameParts(first, last);
    }

    private record NameParts(String firstName, String lastName) {
    }
}
