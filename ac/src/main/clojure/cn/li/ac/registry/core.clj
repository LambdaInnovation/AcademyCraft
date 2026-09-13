(ns cn.li.ac.registry.core
  "Core contracts for AC content discovery.

  Content providers are plain function maps with keys:
    :provider-id    (fn [] -> stable unique id)
    :priority       (fn [] -> sort priority, lower loads first)
    :content-phases (fn [] -> vector of content phase specs)")

;; ============================================================================
;; Key set documentation
;; ============================================================================

(def content-provider-keys
  "Keys required by a content provider function map."
  [:provider-id :priority :content-phases])

;; ============================================================================
;; Wrapper functions
;; ============================================================================

(defn provider-id
  "Get the stable unique id from a provider."
  [provider]
  (if-let [f (:provider-id provider)]
    (f)
    (:id provider)))

(defn priority
  "Get the sort priority from a provider."
  [provider]
  (if-let [f (:priority provider)]
    (f)
    (or (:priority-value provider) (:priority provider) 100)))

(defn content-phases
  "Get the content phases from a provider."
  [provider]
  (if-let [f (:content-phases provider)]
    (vec (f))
    (vec (:phases provider []))))

;; ============================================================================
;; Content Provider Factory
;; ============================================================================

(defn- make-content-provider
  "Create a content provider function map from plain data."
  [id priority-value phases]
  {:provider-id (fn [] id)
   :priority (fn [] priority-value)
   :content-phases (fn [] (vec phases))})

(defn content-provider
  "Create a content provider from plain data."
  [{:keys [id phases priority] :or {priority 100}}]
  (when-not (and (some? id) (sequential? phases))
    (throw (IllegalArgumentException. "content-provider: id must be some?, phases must be sequential?")))
  (make-content-provider id priority phases))

;; ============================================================================
;; Type Check
;; ============================================================================

(defn content-provider?
  "Check if value satisfies the content provider contract."
  [value]
  (and (map? value)
       (fn? (:provider-id value))
       (fn? (:priority value))
       (fn? (:content-phases value))))

;; ============================================================================
;; Normalization
;; ============================================================================

(defn normalize-provider
  [value]
  (cond
    (content-provider? value) value
    (map? value) (content-provider value)
    :else (throw (ex-info "Unsupported content provider" {:value value}))))

;; ============================================================================
;; Safe Accessors — fallback for mixed compilation/classloader states
;; ============================================================================

(defn provider-id*
  "Safe provider id accessor.
  Prefers function map dispatch, but falls back to map fields."
  [provider]
  (or (try
        (provider-id provider)
        (catch Throwable _ nil))
      (:id provider)
      (throw (ex-info "Provider has no id" {:provider provider}))))

(defn provider-priority*
  "Safe provider priority accessor with fallback default 100."
  [provider]
  (or (try
        (priority provider)
        (catch Throwable _ nil))
      (:priority-value provider)
      (:priority provider)
      100))

(defn provider-phases*
  "Safe provider phases accessor. Always returns a vector."
  [provider]
  (let [phases (or (try
                     (content-phases provider)
                     (catch Throwable _ nil))
                   (:phases provider)
                   [])]
    (vec phases)))
