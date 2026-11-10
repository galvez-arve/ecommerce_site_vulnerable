package com.example.ecommercewebapp.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * VULNERABLE ON PURPOSE - training lab, see INSTRUCTOR_KEY.md
 *
 * - Login query is built by raw string concatenation -> classic SQL
 *   injection auth bypass (e.g. username: admin' -- ).
 * - Passwords are stored and compared in plaintext.
 * - Different messages for "no such user" vs "wrong password" -> user
 *   enumeration.
 * - No rate limiting / lockout -> unthrottled credential stuffing.
 * - Session ID is not regenerated on login -> session fixation.
 * - Role is handed to the browser as a plain, unsigned cookie that the
 *   AdminAccessFilter trusts -> trivial privilege escalation by editing
 *   a cookie.
 * - No CSRF token anywhere (Spring Security is not even on the classpath).
 * - "next" is carried through login/register and used as a redirect target
 *   with no validation that it's a local path -> open redirect (e.g.
 *   /login?next=https://evil.example can be used in a phishing link that
 *   looks like it points at this site).
 */
@Controller
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String next,
                             Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        model.addAttribute("next", next);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false) String next,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Model model) {
        model.addAttribute("next", next);

        // Intentionally vulnerable: string-built SQL, no PreparedStatement.
        String sql = "SELECT id, username, role, name, lastname FROM users " +
                "WHERE username = '" + username + "' AND password = '" + password + "'";

        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            // Verbose DB error returned straight to the user/attacker.
            model.addAttribute("error", "Database error: " + e.getMessage());
            model.addAttribute("debugSql", sql);
            return "login";
        }

        if (rows.isEmpty()) {
            // User enumeration: tell the caller which half of the credential was wrong.
            Long exists = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM users WHERE username = '" + username + "'", Long.class);
            String msg = (exists != null && exists > 0)
                    ? "Incorrect password for user '" + username + "'."
                    : "No account found for username '" + username + "'.";
            model.addAttribute("error", msg);
            return "login";
        }

        Map<String, Object> user = rows.get(0);

        // Session fixation: we reuse whatever session already exists instead
        // of invalidating + creating a fresh one on privilege change.
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.get("id").toString());
        session.setAttribute("username", user.get("username"));
        session.setAttribute("role", user.get("role"));

        // Client-trusted state: the admin gate (AdminAccessFilter) reads this
        // cookie, not the server-side session, so it can be forged.
        Cookie roleCookie = new Cookie("role", String.valueOf(user.get("role")));
        roleCookie.setPath("/");
        // No HttpOnly, no Secure, no SameSite.
        response.addCookie(roleCookie);

        Cookie userCookie = new Cookie("userId", user.get("id").toString());
        userCookie.setPath("/");
        response.addCookie(userCookie);

        // Open redirect: "next" goes straight into the Location header with
        // no check that it's a same-site path (should validate it starts
        // with "/" and nothing else, e.g. no "//" or "http(s)://").
        if (next != null && !next.isBlank()) {
            return "redirect:" + next;
        }
        return "redirect:/catalog";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        for (String name : new String[]{"role", "userId"}) {
            Cookie c = new Cookie(name, "");
            c.setPath("/");
            c.setMaxAge(0);
            response.addCookie(c);
        }
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String next, Model model) {
        model.addAttribute("next", next);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String name,
                            @RequestParam String lastname,
                            @RequestParam(required = false) String role, // mass assignment
                            @RequestParam(required = false) String next,
                            Model model) {
        // Mass assignment: the form (and anyone scripting the endpoint) can
        // submit role=admin directly; nothing here restricts it to "user".
        String effectiveRole = (role == null || role.isBlank()) ? "user" : role;

        jdbcTemplate.update(
                "INSERT INTO users (id, name, lastname, address, contact_number, email_address, username, password, role) " +
                        "VALUES (gen_random_uuid(), ?, ?, 'Not provided', 'Not provided', ?, ?, ?, ?)",
                name, lastname, username + "@example.com", username, password, effectiveRole);

        if (next != null && !next.isBlank()) {
            // Same unvalidated pass-through as /login - open redirect.
            return "redirect:/login?next=" + next;
        }
        return "redirect:/login";
    }
}
