(ns cn.li.mc262.runtime.registry
  "Shared access to 26.2 built-in registries."
  (:import [net.minecraft.core Registry]
           [net.minecraft.core.registries BuiltInRegistries]))

(defn ^Registry builtin
  "The named built-in registry.

   Hinted: every caller immediately does .getKey / .getValue on the result, and
   without a return type each of those is a reflective call on the hot side of
   item lookups and sound playback."
  [field-name]
  (case field-name
    "ITEM" BuiltInRegistries/ITEM
    "BLOCK" BuiltInRegistries/BLOCK
    "SOUND_EVENT" BuiltInRegistries/SOUND_EVENT
    "ENTITY_TYPE" BuiltInRegistries/ENTITY_TYPE
    (throw (ex-info "Unknown built-in registry" {:field field-name}))))
