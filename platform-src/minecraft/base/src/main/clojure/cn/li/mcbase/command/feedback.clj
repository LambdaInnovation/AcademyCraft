(ns cn.li.mcbase.command.feedback
  "Single shared command feedback implementation, vanilla-aligned.

  translate? true → send Component.translatable(key, args): the CLIENT resolves
  the key against its own lang files (assets/<modid>/lang/<locale>.json), so
  dedicated servers need no language data and each player sees their own
  locale — matching vanilla, where the packet carries key+args only and the
  server never translates player-facing feedback. NOTE: the client-side
  formatter (TranslatableContents) supports only %s / %n$s specifiers.
  translate? false → literal text; args (if any) are applied with format.

  error? true → sendFailure (red). Vanilla API asymmetry: sendSuccess takes
  Supplier<Component> (1.20.1) while sendFailure takes Component directly."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.commands CommandSourceStack]
           [net.minecraft.network.chat Component]))

(defn send-feedback!
  [^CommandSourceStack source message translate? args error?]
  (try
    (let [component (if translate?
                      (Component/translatable (str message) (object-array (or args [])))
                      (Component/literal (str (if (seq args)
                                                (apply format message args)
                                                message))))]
      (if error?
        (.sendFailure source component)
        (.sendSuccess source
                      (reify java.util.function.Supplier
                        (get [_] component))
                      false)))
    (catch Exception e
      (log/stacktrace "Failed to send feedback:" e))))
