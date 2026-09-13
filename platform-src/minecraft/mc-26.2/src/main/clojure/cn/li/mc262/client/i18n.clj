(ns cn.li.mc262.client.i18n
  "Thin re-export of shared client i18n."
  (:require [cn.li.mcbase.client.i18n :as shared]))

(def translate shared/translate)
(def install-client-i18n! shared/install-client-i18n!)
