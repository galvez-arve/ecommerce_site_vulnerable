package com.example.ecommercewebapp.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * VULNERABLE ON PURPOSE - SSRF.
 *
 * Fetches whatever URL it is given, server-side, with no scheme allowlist,
 * no host allowlist, and no block on private/link-local addresses. Used by
 * the admin "add product by image URL" feature, but reachable directly too.
 * Accepts http(s):// as well as file:// (local file read via SSRF).
 */
@RestController
public class ImageProxyController {

    @GetMapping("/image-proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String url) {
        try {
            URL target = new URL(url); // no scheme/host validation at all
            URLConnection conn = target.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            if (conn instanceof HttpURLConnection http) {
                http.setInstanceFollowRedirects(true); // redirects can repoint anywhere, including internal hosts
            }
            String contentType = conn.getContentType();

            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                int total = 0;
                while ((n = in.read(buf)) != -1 && total < 10_000_000) {
                    out.write(buf, 0, n);
                    total += n;
                }
                byte[] body = out.toByteArray();
                MediaType mediaType = contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM;
                return ResponseEntity.ok().contentType(mediaType).body(body);
            }
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(("Fetch failed: " + e).getBytes());
        }
    }
}
