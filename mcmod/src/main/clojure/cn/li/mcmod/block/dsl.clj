(ns cn.li.mcmod.block.dsl
	"Public aggregate entrypoint for the block DSL.
   
	 The actual implementation now lives in:
	 - cn.li.mcmod.block.dsl-properties
	 - cn.li.mcmod.block.dsl-multiblock
	 - cn.li.mcmod.block.dsl-templates
	 - cn.li.mcmod.block.dsl-validators
	 - cn.li.mcmod.block.dsl-core
	 - cn.li.mcmod.block.query
   
	 This namespace is intentionally retained as the user-facing DSL surface;
	 implementation details remain in the focused modules above."
	(:require [cn.li.mcmod.block.dsl-properties :as props]
						[cn.li.mcmod.block.dsl-multiblock :as mb]
						[cn.li.mcmod.block.dsl-templates :as templates]
						[cn.li.mcmod.block.dsl-validators :as validators]
						[cn.li.mcmod.block.dsl-core :as core]
						[cn.li.mcmod.block.query :as q]))

(def materials props/materials)
(def sound-types props/sound-types)
(def tool-types props/tool-types)
(def default-hardness props/default-hardness)
(def default-resistance props/default-resistance)
(def default-light-level props/default-light-level)
(def default-friction props/default-friction)
(def default-creative-tab props/default-creative-tab)

(defn block-registry []
	(core/get-block-registry))

(def get-block-spec q/get-block-spec)
(def list-blocks q/list-all-blocks)
(def controller-parts-block? q/controller-parts-block?)
(def is-controller-block? q/is-controller-block?)
(def is-part-block? q/is-part-block?)
(def is-multi-block? q/is-multi-block?)
(def is-light-emitter? q/is-light-emitter?)
(def get-controller-block-id q/get-controller-block-id)
(def get-part-block-id q/get-part-block-id)
(def get-tile-kind q/get-tile-kind)
(def get-light-level q/get-light-level)
(def get-hardness q/get-hardness)
(def get-material q/get-material)
(def get-structure-offsets q/get-structure-offsets)
(def has-block-state-properties? q/has-block-state-properties?)
(def has-block-entity? q/has-block-entity?)

(def calculate-multi-block-positions mb/calculate-multi-block-positions)
(def normalize-positions mb/normalize-positions)
(def validate-multi-block-positions mb/validate-multi-block-positions)
(def get-multi-block-master-pos mb/get-multi-block-master-pos)
(def resolve-multi-block-master-pos mb/resolve-multi-block-master-pos)
(def all-multi-block-positions mb/all-multi-block-positions)
(def can-place-multi-block? mb/can-place-multi-block?)
(def is-multi-block-complete? mb/is-multi-block-complete?)
(def create-cross-shape mb/create-cross-shape)
(def create-l-shape mb/create-l-shape)
(def create-t-shape mb/create-t-shape)
(def create-pyramid-shape mb/create-pyramid-shape)
(def create-hollow-cube mb/create-hollow-cube)

(def create-block-spec core/create-block-spec)
(def register-block! core/register-block!)
(def get-block-properties core/get-block-properties)

(def ore-template templates/ore-template)
(def wood-template templates/wood-template)
(def metal-template templates/metal-template)
(def glass-template templates/glass-template)
(def light-block-template templates/light-block-template)
(def multi-block-template templates/multi-block-template)
(def irregular-multi-block-template templates/irregular-multi-block-template)
(def merge-templates templates/merge-templates)

(def validate-block-spec validators/validate-block-spec)

(defmacro defblock [block-name & options]
	`(core/defblock ~block-name ~@options))

(defmacro defmultiblock [base-name & options]
	`(core/defmultiblock ~base-name ~@options))

(defmacro defblock-template [block-name config]
	`(core/defblock-template ~block-name ~config))