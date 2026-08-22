package app.adapters.input.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

/** The two server-rendered pages behind the form login; everything else is the React client. */
@Controller
public class WebController {

    /** Renders the form-login page. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Renders the landing page with the signed-in name, roles, and whether they are an admin. */
    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .collect(Collectors.joining(", ")));
        model.addAttribute("admin", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
        return "home";
    }
}
