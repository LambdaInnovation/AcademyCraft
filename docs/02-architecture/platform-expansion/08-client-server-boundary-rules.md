# Client/server boundary rules

- Server code must not statically require client-only namespaces.
- Client code belongs in loader client components or Minecraft client components.
- Loader lifecycle code belongs in loader components.
- Minecraft components provide Minecraft API adaptation without enumerating loaders.
- Shared core projects (`api`, `mcmod`, `ac`) must remain loader-neutral.

Use `verifyCurrentPlatforms` before committing boundary-sensitive changes.
