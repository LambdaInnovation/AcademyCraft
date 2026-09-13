(ns hooks.defskill
  (:require [clj-kondo.hooks-api :as api]))

(defn defskill [{:keys [node]}]
  (let [[_category sym & _body] (:children node)]
    ;; Replace (defskill cat sym ...) with (def sym nil)
    ;; — registers the var without bogus "Too many arguments to def" errors
    {:node (api/list-node
             [(api/token-node 'def)
              sym
              (api/token-node nil)])}))

(defn- declare-vars
  [symbols]
  (api/list-node
   (into [(api/token-node 'declare)]
         (map api/token-node symbols))))

(defn def-skill-config-ops
  "Expose the private helper vars emitted by def-skill-config-ops to kondo.

  The production macro intentionally emits these vars into each caller's
  namespace, so a plain :lint-as mapping cannot describe them.  A declaration
  is sufficient for analysis and leaves the runtime macro expansion intact."
  [_]
  {:node (declare-vars
          ['cfg-double 'cfg-int 'cfg-boolean 'cfg-lerp 'cfg-lerp-int
           'cfg-double-list 'cfg-string-set 'cfg-probability 'cfg-progression
           'skill-exp])})

(defn arc-beam-fx
  "Expose the four vars emitted by def-arc-beam-fx to kondo."
  [_]
  {:node (declare-vars
          ['init! 'fx-snapshot 'reset-fx-for-test! 'clear-fx-owner!])})
