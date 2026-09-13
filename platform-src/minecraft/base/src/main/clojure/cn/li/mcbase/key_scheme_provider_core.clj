(ns cn.li.mcbase.key-scheme-provider-core
  "Shared key state provider implementation for Forge/Fabric/NeoForge.

   Implements mcmod KeySchemeProvider SPI by querying keyboard/mouse state
   through Minecraft window + GLFW."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McClientAccess]
           [com.mojang.blaze3d.platform InputConstants$Type]
           [net.minecraft.client Minecraft]))

(defn- get-window-handle
  "Get current Minecraft GLFW window handle."
  []
  (let [mc (Minecraft/getInstance)
        window (.getWindow mc)]
    (McClientAccess/windowHandle window)))

(defn- key-down?
  "Query GLFW key state for a keyboard key code.

   Returns true when key is pressed."
  [key-code]
  (try
    (let [window-handle (get-window-handle)]
      (= 1 (org.lwjgl.glfw.GLFW/glfwGetKey window-handle (int key-code))))
    (catch Throwable e
      (log/debug e "Failed to query GLFW key state" {:key-code key-code})
      false)))

(defn- mouse-button-down?
  "Query GLFW mouse button state (GLFW_MOUSE_BUTTON_LEFT=0, _RIGHT=1, ...).

   Returns true when the button is pressed."
  [button-idx]
  (try
    (let [window-handle (get-window-handle)]
      (= 1 (org.lwjgl.glfw.GLFW/glfwGetMouseButton window-handle (int button-idx))))
    (catch Throwable e
      (log/debug e "Failed to query GLFW mouse button state" {:button-idx button-idx})
      false)))

(def ^:private mouse-button-keywords
  {:mouse-left 0
   :mouse-right 1})

(defn key-display-name
  "Localized display name for the integer convention used by AC Settings."
  [key-code]
  (case (int key-code)
    -100 "MOUSE 1"
    -99 "MOUSE 2"
    (.getString
      (.getDisplayName
        (.getOrCreate InputConstants$Type/KEYSYM (int key-code))))))

(def ^:private impl
  {:is-key-down?
   (fn [scheme-name key-idx]
     (cond
       (contains? mouse-button-keywords key-idx)
       (mouse-button-down? (get mouse-button-keywords key-idx))
       (and (= :original scheme-name) (integer? key-idx))
       (key-down? key-idx)
       (integer? key-idx)
       (key-down? key-idx)
       :else
       false))})

(defn get-spi-implementation
  "Get KeySchemeProvider SPI implementation object for platform installation."
  []
  impl)
