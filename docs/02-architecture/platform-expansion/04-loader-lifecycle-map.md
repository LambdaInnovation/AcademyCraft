# Loader lifecycle map

## Forge target

1. Forge Java entrypoint is loaded by Forge.
2. The Java entrypoint requires the Forge Clojure entry namespace.
3. The Forge Clojure entry calls shared platform bootstrap.
4. Forge-owned registry, event, client, config, and datagen bindings are installed from the Forge loader component.

## Fabric target

1. Fabric Java entrypoints are loaded by Fabric.
2. The Java entrypoints require Fabric Clojure entry namespaces.
3. Fabric Clojure entry functions call shared platform bootstrap.
4. Fabric-owned registry, event, client, and datagen bindings are installed from the Fabric loader component.

## New loader work

Do not create a real new-loader target for architecture probing. Validate
catalog, source-set, AOT, capability, and artifact rules with focused unit tests
against the single production catalog.
