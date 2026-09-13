(ns cn.li.mcbase.client.render.script-render-compiler
  "Compile validated ScriptRender profiles into immutable draw plans.

  Compiler runs outside render hot paths. Renderers should consume draw plans
  directly without re-validating or re-normalizing profile maps."
  (:require [cn.li.platform.neutral.client-render :as abi]))

(def ^:private compiled-state-keys [:depth-test :blend :cull :layer])

(defn compile-profile
  [profile]
  (let [normalized (abi/validate-profile! profile)]
    {:id (:id normalized)
     :kind (:kind normalized)
     :version (:version normalized)
     :enabled? (:enabled? normalized)
     :state (select-keys (:state normalized) compiled-state-keys)
     :anim (or (:anim normalized) {})
     :params (or (:params normalized) {})
     :budget (or (:budget normalized) {})}))

(defn compile-profiles
  [profile-map]
  (reduce-kv (fn [acc profile-id profile]
               (assoc acc profile-id (compile-profile profile)))
             {}
             (or profile-map {})))
