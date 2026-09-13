(ns cn.li.mcmod.i18n
  "Platform-neutral i18n entrypoint.

  - `ac` / `mcmod` code should depend on THIS namespace only.
  - Platform modules (forge/fabric) should set `*translate-fn*` at init time."
  (:refer-clojure :exclude [format]))

(def ^:dynamic *translate-fn*
  "Function (fn [key args] -> string). `args` is a (possibly empty) seq of format
   arguments passed to the platform formatter. Defaults to returning the key;
   platform modules alter-var-root this at init."
  (fn [k _args] (str k)))

(def ^:dynamic *current-lang-fn*
  "Function (fn [] -> language code string) resolving the client's current
   language (e.g. \"zh_cn\"). nil until a platform module installs it."
  nil)

(defn current-language-code
  "Current client language code (e.g. \"zh_cn\", \"en_us\"), or nil when no
   platform fn is installed (server side, tests).  Consumers normalize case
   as needed."
  []
  (when *current-lang-fn*
    (*current-lang-fn*)))

(defn translate
  "Translate a language key. Extra args are handed to the platform formatter
   (Minecraft's I18n.get) to fill the language file's %s/%d specifiers — so the
   i18n layer does the formatting, honouring locale + positional (%1$s) args.
   With no args the raw translation is returned."
  [k & args]
  (*translate-fn* k args))

