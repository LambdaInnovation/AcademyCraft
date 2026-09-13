(ns cn.li.mcmod.block.dsl-validators
  "Block specification validation.
   Validates block specs, properties, and multi-block configurations."
  (:require [clojure.string :as str]
            [cn.li.mcmod.block.dsl-properties :as props]
            [cn.li.mcmod.block.dsl-multiblock :as mb]))

;; ============================================================================
;; Block Specification Validation
;; ============================================================================

(defn validate-block-spec
  "Validate a complete block specification for correctness.
   Throws ex-info if validation fails."
  [block-spec]
  (when-not (:id block-spec)
    (throw (ex-info "Block must have an :id" {:spec block-spec})))
  (when-not (string? (:id block-spec))
    (throw (ex-info "Block :id must be a string" {:id (:id block-spec)})))
  (when (and (:registry-name block-spec)
             (or (not (string? (:registry-name block-spec)))
                 (str/blank? (:registry-name block-spec))))
    (throw (ex-info "Block :registry-name must be a non-empty string when provided"
                    {:registry-name (:registry-name block-spec)
                     :id (:id block-spec)})))

  ;; Validate physical properties
  (let [physical (:physical block-spec)]
    (when-not (get props/materials (:material physical))
      (throw (ex-info "Invalid material" {:material (:material physical)
                                          :valid props/materials
                                          :id (:id block-spec)}))))

  ;; Validate rendering properties
  (let [rendering (:rendering block-spec)]
    (when (and (:model-parent rendering)
               (or (not (string? (:model-parent rendering)))
                   (str/blank? (:model-parent rendering))))
      (throw (ex-info "Block :model-parent must be a non-empty string when provided"
                      {:model-parent (:model-parent rendering)
                       :id (:id block-spec)})))

    (when (and (:textures rendering)
               (not (map? (:textures rendering))))
      (throw (ex-info "Block :textures must be a map when provided"
                      {:textures (:textures rendering)
                       :id (:id block-spec)})))

    (when (and (:model-textures rendering)
               (not (map? (:model-textures rendering))))
      (throw (ex-info "Block :model-textures must be a map when provided"
                      {:model-textures (:model-textures rendering)
                       :id (:id block-spec)})))

    (when (map? (:model-textures rendering))
      (doseq [[model-name texture-path] (:model-textures rendering)]
        (when-not (or (string? model-name) (keyword? model-name))
          (throw (ex-info "Block :model-textures keys must be string/keyword"
                          {:invalid-key model-name
                           :id (:id block-spec)})))
        (when-not (and (string? texture-path) (not (str/blank? texture-path)))
          (throw (ex-info "Block :model-textures values must be non-empty texture path strings"
                          {:invalid-value texture-path
                           :model-name model-name
                           :id (:id block-spec)})))))

    (let [render-shape (:render-shape rendering)
          valid-render-shapes #{:model :invisible :entityblock-animated}]
      (when (and (some? render-shape)
                 (not (contains? valid-render-shapes render-shape)))
        (throw (ex-info "Block :render-shape must be one of :model, :invisible, :entityblock-animated"
                        {:render-shape render-shape
                         :id (:id block-spec)})))))

  ;; Validate multi-block configuration
  (let [multi-block (:multi-block block-spec)]
    (when (:multi-block? multi-block)
      (let [has-size? (:multi-block-size multi-block)
            has-positions? (:multi-block-positions multi-block)]
        ;; Must have either size (regular) or positions (irregular)
        (when-not (or has-size? has-positions?)
          (throw (ex-info "Multi-block must have either :multi-block-size or :multi-block-positions"
                          {:id (:id block-spec)})))
        ;; Validate regular multi-block size
        (when has-size?
          (let [{:keys [width height depth]} (:multi-block-size multi-block)]
            (when-not (and width height depth
                           (pos? width) (pos? height) (pos? depth))
              (throw (ex-info "Invalid multi-block size, must have positive :width :height :depth"
                              {:id (:id block-spec)
                               :size (:multi-block-size multi-block)})))))
        ;; Validate irregular multi-block positions
        (when has-positions?
          (mb/validate-multi-block-positions (:multi-block-positions multi-block))))))
  true)
