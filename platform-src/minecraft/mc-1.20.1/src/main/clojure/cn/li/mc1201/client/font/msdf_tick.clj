(ns cn.li.mc1201.client.font.msdf-tick
  "Client-tick hooks for MSDF font follow-ups (glow animation, etc.)."
  (:require [cn.li.mc1201.client.font.msdf-setup :as msdf-setup])
  (:import [cn.li.mc1201.client.font.msdf MsdfFontManager]))

(defn client-tick!
  "Advance MSDF client-side animations and retry deferred font init."
  []
  (when-not (MsdfFontManager/isAvailable)
    (msdf-setup/ensure-ready!))
  (when (MsdfFontManager/hasFontFace)
    (MsdfFontManager/clientTick)))
