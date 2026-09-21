# adr 0004: jwt with a shared secret for the demo

status: accepted

## context
the api must be protected and role based (accountant, admin). a full setup
would use an identity provider like keycloak or auth0 and verify tokens against
its public keys (issuer-uri). that is a lot to run just to try the project.

## decision
for local use we verify tokens signed with one shared secret (HMAC). a small
`/dev/token` endpoint hands out a ready token so you can call the api with curl
or postman right away. this endpoint is turned off in the prod profile.

## why
- anyone can clone the repo and try the secured api in one minute, no extra
  server needed.
- the security code (resource server, roles from the `roles` claim,
  `@PreAuthorize`) is exactly the same as with a real provider. only where the
  key comes from changes.

## trade-off
a shared secret is weaker than real public/private keys and has no central
logout or rotation. that is why it is dev only. moving to keycloak is a config
change: swap the `JwtDecoder` for one built from `issuer-uri`.
