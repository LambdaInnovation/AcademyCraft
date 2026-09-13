(ns cn.li.mc1211.client.font.msdf-setup
  "MSDF shadow font initialization (replaces TTF virtual pack).

  Detects a system TrueType font via pure file IO, loads it with STB, and
  builds the isolated MSDF FontSet used by CGUI text rendering.

  Init is retried until both the font face and msdf_text shader are ready."
  (:require [cn.li.mcbase.client.font.system-font-detector :as detector]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mc1211.client.font.msdf MsdfFontManager]))

(defn ensure-ready!
  "Load MSDF face when possible. Safe to call every frame until `isAvailable`."
  []
  (try
    (when-not (MsdfFontManager/hasFontFace)
      (when-let [{:keys [path]} (detector/detect-system-font)]
        (when-not (MsdfFontManager/init path)
          (log/debug "MSDF face init deferred (client not ready or font load failed)"))))
    (catch Exception e
      (log/debug "MSDF ensure-ready failed: %s" (ex-message e))))
  nil)

(defn on-shader-ready!
  "Called from platform shader registration after academy:msdf_text is loaded."
  []
  (log/debug "MSDF text shader registered")
  (ensure-ready!))

(defn init!
  "First-chance MSDF setup during client bootstrap (may retry later)."
  []
  (ensure-ready!))

(defn msdf-ready?
  []
  (MsdfFontManager/hasFontFace))

(defn has-font-face?
  []
  (MsdfFontManager/hasFontFace))
