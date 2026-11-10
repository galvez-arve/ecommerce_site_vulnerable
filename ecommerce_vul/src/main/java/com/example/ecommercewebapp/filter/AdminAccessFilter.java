package com.example.ecommercewebapp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * VULNERABLE ON PURPOSE.
 *
 * "Protects" the admin HTML pages (/users, /products, /orders, /admin/**)
 * by checking an unsigned "role" cookie set at login. Two deliberate flaws:
 *
 *  1. The check trusts client-supplied state. Anyone can open devtools,
 *     set a cookie named "role" to "admin", and reload - no server-side
 *     session or signature is verified.
 *  2. It only guards the *page* routes. The JSON APIs those pages call
 *     (/api/users, /api/products, /api/orders, /api/analytics/**) are
 *     matched separately below and intentionally left unauthenticated,
 *     so the data is reachable directly with curl even without the
 *     cookie trick ("security enforced only in the UI").
 */
@Component
public class AdminAccessFilter implements Filter {

    private static final Set<String> PROTECTED_PAGES =
            Set.of("/dashboard", "/users", "/products", "/orders", "/analytics");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        boolean isAdminPage = PROTECTED_PAGES.contains(path) || path.startsWith("/admin/");

        if (isAdminPage) {
            String role = readCookie(request, "role");
            if (!"admin".equals(role)) {
                response.sendRedirect("/login?error=" +
                        java.net.URLEncoder.encode("Admin access required", "UTF-8"));
                return;
            }
        }

        // Everything under /api/** (including /api/users, /api/orders,
        // /api/products, /api/analytics/**) intentionally passes straight
        // through with no check at all.
        chain.doFilter(req, res);
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
