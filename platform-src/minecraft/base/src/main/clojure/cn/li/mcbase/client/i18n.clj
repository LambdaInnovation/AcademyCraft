(ns cn.li.mcbase.client.i18n
  "CLIENT-ONLY shared i18n implementation."
  (:require [cn.li.mcmod.i18n :as i18n])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.resources.language I18n]
           [net.minecraft.locale Language]))

(defn translate
  ;; With args, let Minecraft's I18n.get format the string (locale-aware, honours
  ;; %s/%d and positional %1$s). With no args, return the RAW translation via
  ;; Language.getOrDefault — I18n.get would run String.format on a %s-containing
  ;; value with zero args and yield Minecraft's "Format error: ..." string.
  [k args]
  (try
    (if (seq args)
      (I18n/get (str k) (to-array args))
      (.getOrDefault (Language/getInstance) (str k)))
    (catch Throwable _
      (str k))))

(defn install-client-i18n!
  []
  (alter-var-root #'i18n/*translate-fn* (constantly translate))
  (alter-var-root #'i18n/*current-lang-fn*
                  (constantly (fn []
                                (try
                                  (some-> (Minecraft/getInstance) (.options) (.languageCode))
                                  (catch Throwable _ nil)))))
  nil)
