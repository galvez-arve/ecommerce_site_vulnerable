VULNLAB ECOMMERCE - WHAT WAS ADDED AND WHY IT IS VULNERABLE
=============================================================

This is your existing Spring Boot + PostgreSQL ecommerce repo with a login
page, an admin area, and a public shop (catalog, product, checkout) layered
on top. Every new piece is deliberately insecure for classroom bug hunting
with Nuclei (and manual testing). I did not change your existing data model
beyond adding a few nullable columns, and the original restorepoint.sql
dataset is still used as-is.

I could not actually build or run this project in my sandbox (no Maven/
internet access to Maven Central there), so compile and run it yourself
before class - see "First run" below - and tell me if anything needs fixing.

FIRST RUN
---------
1. docker compose up -d --build
2. App: http://127.0.0.1:8084   Postgres: 127.0.0.1:5432 (postgres / P@ssw0rd123)
3. Everything is bound to 127.0.0.1 only - keep it that way for an isolated
   lab network, never expose it publicly.
4. Reset between classes: docker compose down -v && docker compose up -d --build
   (this re-runs db-init/00-restore.sh and the migration SQL from scratch)

ACCOUNTS (all plaintext passwords, seeded by db-init/01-migrate.sql)
----------------------------------------------------------------------
  admin    / admin123    (role: admin)
  manager  / manager123  (role: admin)
  testuser / password1   (role: user)
  Every existing customer from restorepoint.sql also got an account:
  username = the part of their email before "@", password = "Password123"

PAGES
-----
  /login, /register        - public auth
  /catalog                 - public product catalog (images, search)
  /catalog/product?id=     - public product detail
  /checkout, /order/{id}   - public cart / checkout / order view
  /products /users /orders - admin pages (same URLs as before), now
                             "gated" by AdminAccessFilter
  /admin/products/new      - admin add-product form
  /image-proxy?url=        - fetch-any-URL endpoint (used by the admin form,
                             also directly reachable)

VULNERABILITIES BY CLASS
-------------------------

1. Authentication (AuthController)
   - SQL injection auth bypass: the login query is built by string
     concatenation. Try username: admin' -- with any password.
   - Plaintext password storage and comparison.
   - User enumeration: "no such user" vs "wrong password" are different
     messages.
   - No rate limiting / lockout on /login.
   - Session fixation: session ID is not rotated on login.
   - Mass assignment on /register: the form posts a hidden "role" field the
     server trusts outright - change it to "admin" and register as admin
     directly (POST a different value than the form's default).

2. Broken access control (AdminAccessFilter)
   - The admin pages (/products, /users, /orders, /admin/**) are "protected"
     by reading an unsigned "role" cookie set at login. Open devtools, add a
     cookie role=admin, and you're in - no server-side session or signature
     check.
   - The filter only guards the HTML pages. The JSON APIs those pages call
     (/api/users, /api/products, /api/orders, /api/analytics/**) have zero
     protection at all - curl them directly, no cookie needed. This is the
     classic "auth only in the UI, not the API" bug.
   - /api/users leaks every user's PII (address, contact number, email) to
     anyone, logged in or not.
   - /order/{id} shows any order's full details (name, email, items, notes)
     to anyone who has or guesses the ID - IDOR, no ownership check.

3. SQL injection (two more vectors beyond login)
   - /catalog/product?id=  - string-concatenated query, returns DB errors
     and (via UNION SELECT) extra columns/rows straight to the page.
   - Try: /catalog/product?id=' OR '1'='1
   - Try: /catalog/product?id=' UNION SELECT username,password,role,
     'leak','leak' FROM users --   (adjust column count/types to the
     products query: id, product_name, expiration_date, voucher_discount,
     no_of_quantity, product_price, image_url)

4. Cross-site scripting
   - Reflected: /catalog?q=<img src=x onerror=alert(1)> (rendered with
     th:utext on the catalog page).
   - Reflected: /catalog/product?id=<script>...</script> (the "no product
     found for id: ..." message is also th:utext).
   - Stored: the checkout "Delivery notes" field (shippingNotes) is stored
     as-is and rendered with th:utext on the order confirmation page, and
     via innerHTML in the admin Orders page's new "Notes" column, and in
     the JSON the admin page fetches from /api/orders.
   - Stored (secondary path): a malicious product name saved through the
     admin "Add Product" form is rendered via innerHTML on the public
     catalog page for every visitor.

5. Business logic / price tampering (CheckoutController)
   - /cart/add trusts a client-supplied "price" and "discount" hidden field
     with no re-check against the products table.
   - /checkout accepts an "overrideTotal" field that replaces the computed
     total outright - set it to 0.01, or negative, and see what the server
     does with it.
   - /checkout accepts "orderStatus" directly - an order can be submitted
     pre-marked "Delivered", skipping any real fulfillment workflow.
   - /checkout accepts "userId" directly, letting you place (and later
     view) an order attributed to any existing user id, not just your own
     session - ties into the IDOR on /order/{id}.
   - No CSRF token anywhere, so all of the above is also a CSRF target.

6. SSRF (ImageProxyController)
   - GET /image-proxy?url=<anything> fetches server-side with no scheme or
     host allowlist. Try internal docker hostnames (http://db:5432),
     cloud metadata-style addresses, or file:// URLs for local file read.
   - It's exposed directly, and also offered as a "preview" helper from the
     admin add-product form.

7. Unrestricted / path-traversal file upload (AdminProductController)
   - The "Or upload an image file" field on /admin/products/new has no
     extension or content-type check, and uses the uploaded filename
     exactly as sent. A filename containing "../" escapes the uploads
     folder; a .html or .svg file with embedded <script> is stored and
     served back as-is from /uploads/<name> (stored XSS via upload).

8. Information disclosure / misconfiguration
   - Spring Boot Actuator is fully exposed with no authentication:
     /actuator/env (leaks the DB password and anything else in the
     environment), /actuator/heapdump, /actuator/beans, /actuator/mappings,
     /actuator/health (full details).
   - server.error.include-stacktrace=always - any unhandled exception
     (e.g. a malformed SQLi payload) returns a full Java stack trace.
   - .env, docker-compose.yml, restorepoint.sql and application.properties
     all contain the same hardcoded DB password (P@ssw0rd123) - consistent
     with your original repo; worth pointing out as a finding in its own
     right (credentials reused across config surfaces).
   - src/main/resources/templates/fragments/chat-widget.html hardcodes an
     internal webhook URL with a private IP (192.168.55.221) - an existing
     internal-network disclosure in your original repo, left as-is.

SUGGESTED STUDENT WORKFLOW
---------------------------
1. nuclei -u http://127.0.0.1:8084 -tags exposure,misconfig,config,cors
2. nuclei -u http://127.0.0.1:8084 -tags sqli,xss,ssrf,lfi -severity low,medium,high,critical
3. Build a DAST target list (requires params) and run:
     nuclei -l urls.txt -dast
   Include at least:
     http://127.0.0.1:8084/login (POST username/password)
     http://127.0.0.1:8084/catalog?q=test
     http://127.0.0.1:8084/catalog/product?id=1
     http://127.0.0.1:8084/image-proxy?url=http://example.com
4. Manually test what Nuclei is unlikely to find: the cookie-based admin
   bypass, the checkout price/userId/orderStatus tampering, the mass
   assignment on registration, and the stored XSS via checkout notes -
   these require understanding the business logic, not just pattern
   matching a response.
5. Have students write up each finding (impact, reproduction steps, fix),
   then patch one and rescan to confirm it's gone.

WHAT I'D STILL LIKE TO CONFIRM WITH YOU
-----------------------------------------
- I have not compiled or run this (no Maven/internet access on my end) -
  please `docker compose up -d --build` and sanity-check the flow (login,
  browse catalog, add to cart, checkout, view admin orders/notes XSS)
  before using it with students, and let me know what breaks.
- Let me know if you want a "fixed" branch/version too, for a patch-and-
  rescan exercise.
