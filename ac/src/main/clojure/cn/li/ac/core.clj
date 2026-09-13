(ns cn.li.ac.core
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.lifecycle :as lifecycle]
            [cn.li.mcmod.spi.entity-render-registry :as entity-render-registry]
            [cn.li.mcmod.spi.entity-behavior-registry :as entity-behavior-registry]
            [cn.li.ac.bootstrap :as ac-bootstrap]
            [cn.li.ac.core.init :as core-init]
            [cn.li.ac.core.content-loader :as content-loader]
            [cn.li.ac.wireless.data.world :as wireless-world]
            [cn.li.ac.media.external-scan :as media-external-scan]
            [cn.li.ac.terminal.client.actions :as terminal-actions]
            [cn.li.ac.terminal.client.install-effect-reactive :as install-effect-reactive]
            [cn.li.ac.client.platform-hooks :as platform-hooks]
            [cn.li.ac.client.font-init :as font-init]
            [cn.li.ac.datagen.bootstrap :as datagen-bootstrap]
            [cn.li.ac.registry.hooks :as hooks]
            [cn.li.ac.testing.smoke-manifest :as smoke-manifest]
            [cn.li.ac.tutorial.content :as tut-content]
            [cn.li.mcmod.i18n :as i18n]))

;; Lifecycle hooks guard — Framework [:service :ac-lifecycle-hooks]

(def ^:private guard-path [:service :ac-lifecycle-hooks])

(defn lifecycle-hooks-guard-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (boolean (get-in @fw-atom guard-path))
    false))

(defn reset-lifecycle-hooks-guard-for-test!
  ([]
   (reset-lifecycle-hooks-guard-for-test! false))
  ([registered?]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in guard-path (boolean registered?)))
   nil))

(defn init
  []
  (core-init/init))

(defn activate-runtime-content!
  []
  (content-loader/activate-runtime-content!))

(defn register-datagen-metadata!
  []
  (datagen-bootstrap/register-datagen-metadata!))

;; Register client-side initialization callback
(defn- init-client-renderers
  "Run content-owned client renderer initialization.
  Called by mcmod during client initialization."
  []
  ;; Register ability FX channels/runtimes before any context-channel push can
  ;; arrive. This bootstrap used to exist only in tests, leaving the production
  ;; FX registry empty: held skills still ran on the server, but their client
  ;; beam/sound start events had no handler.
  ;; Resolve lazily so the client-only FX graph is never loaded while the
  ;; dedicated server requires this shared lifecycle namespace.
  ((requiring-resolve 'cn.li.ac.content.ability-client/init-client-fx!))
  ;; Register entity render namespaces into the neutral mcmod registry
  ;; so that Minecraft-version Java renderer classes can resolve them without
  ;; hardcoding AC namespace strings.
  (entity-render-registry/register-entity-render-ns!
    "silbarn" "cn.li.ac.content.entities.silbarn-render")
  ;; Keyed by the entity's registry name: the mag hook is a scripted
  ;; projectile, so it has no block-body hook id.
  (entity-render-registry/register-entity-render-ns!
    "entity_mag_hook" "cn.li.ac.entity.mag-hook-render")
  (entity-behavior-registry/register-behavior!
    :impact-detonation
    {:gravity-delay-ticks 50
     :despawn-delay-ticks 10
     :heavy-sound "entity.silbarn_heavy"
     :light-sound "entity.silbarn_light"
     :particle "silbarn_frag"})
  ;; cn.li.ac.terminal.client.actions/install-ui-hooks!
  (terminal-actions/install-ui-hooks!)
  ;; Push handler for terminal install-effect (moved out of shell to break circular dep)
  (install-effect-reactive/install-push-handler!)
  (platform-hooks/install-client-content-actions!)
  (font-init/init-fonts!)
  ;; Tutorial content (title/brief/body markdown) must follow the client's
  ;; language, not default to en_US: the platform exposes the current language
  ;; code through the neutral i18n layer.
  (tut-content/install-current-lang-fn! #(i18n/current-language-code))
  (hooks/load-all-client-renderers!)
  (media-external-scan/rescan!))

(defn- register-lifecycle-hooks-body!
  []
  (smoke-manifest/register!)
  (ac-bootstrap/register-post-spi-init!)
  (lifecycle/register-content-init! #'init)
  (lifecycle/register-runtime-content-activation! #'activate-runtime-content!)
  (lifecycle/register-world-tick! wireless-world/on-world-tick)
  (lifecycle/register-datagen-metadata-init! #'register-datagen-metadata!)
  (lifecycle/register-client-init! init-client-renderers))

(defn register-lifecycle-hooks!
  "Register AC lifecycle hooks with mcmod.

  This is the entrypoint named by generated suite metadata.
  Requiring this namespace alone must not mutate lifecycle
  state."
  []
  (let [should-register?
        (if-let [fw-atom (fw/fw-atom)]
          (let [k guard-path]
            (when-not (true? (get-in @fw-atom k))
              (swap! fw-atom assoc-in k true)
              true))
          true)]
    (when should-register?
      (register-lifecycle-hooks-body!)))
  nil)

(defn runtime-provider
  "Neutral content-provider factory loaded from generated target metadata.

   The returned function values intentionally contain no Minecraft or loader
   classes; platform code supplies those capabilities through host ports."
  [_context]
  {:register-content! register-lifecycle-hooks!})
