(ns cn.li.mcmod.client.render.script-render-abi
	"Shared ScriptRender ABI contract and validation.

	This namespace is platform-agnostic and intentionally data-only. Runtime
	executors should consume validated profiles without additional shape checks
	in hot render loops."
	(:require [clojure.set :as set]
			  [cn.li.mcmod.framework :as fw]
			  [cn.li.mcmod.framework.registry :as registry]))

(def ^:private builtin-kinds
	#{:billboard-cross
	  :animated-billboard
	  :spinning-double-sided
	  :ring-lines
	  :polyline-arc
	  :wire-box
	  :ray-composite
	  :ray-composite-lite})

(defn register-scripted-effect-kind!
	"Register an additional scripted-effect kind. Content modules call this during init."
	[kind]
	(registry/register! (fw/fw-atom) :render [::kind kind] true)
	nil)

(defn supported-kinds
	"Return the set of currently registered scripted-effect kinds."
	[]
	(if-let [fw-atom (fw/fw-atom)]
		(into builtin-kinds
			(keep (fn [[k _]] (when (and (vector? k) (= ::kind (first k))) (second k))))
			(get-in @fw-atom [:registry :render]))
		builtin-kinds))

;; --- kind → native renderer key mapping ---
;; Content modules map their kind keywords to platform-neutral renderer keys.
;; The Minecraft renderer dispatcher uses these keys (not raw kind names) in
;; switch statements, keeping content-owned kind strings out of the platform layer.

(defn register-kind-renderer-key!
	"Map a scripted-effect kind to a platform-neutral renderer key.
	 kind is a keyword (e.g. :custom-kind), renderer-key is a keyword
	 (e.g. :tiered-zigzag). Called by content during init."
	[kind renderer-key]
	(registry/register! (fw/fw-atom) :render [::renderer-key kind] renderer-key)
	nil)

(defn resolve-kind-renderer-key
	"Return the renderer key for a given kind, or the kind itself if no mapping exists.
	 kind can be a keyword or string."
	[kind]
	(let [k (if (keyword? kind) kind (keyword kind))]
	(or (registry/get-spec (fw/fw-atom) :render [::renderer-key k]) k)))

(def ^:private allowed-top-level-keys
	#{:id :kind :version :enabled? :state :anim :params :budget})

(def ^:private allowed-state-keys
	#{:depth-test :blend :cull :layer})

(def ^:private allowed-depth-test
	#{:default :force-on :force-off})

(def ^:private allowed-blend
	#{:alpha :additive})

(def ^:private allowed-cull
	#{:default :off})

(def ^:private allowed-layer
	#{:lines :translucent})

(defn kind-default-params
	[kind]
	(case kind
		:billboard-cross {:size 0.6
											:color-r 180
											:color-g 220
											:color-b 255}
		:animated-billboard {:texture-prefix ""
											 :frame-count 1
											 :frame-ms 50.0
											 :half-size 0.5
											 :offset-y 0.0
											 :offset-z 0.0}
		:spinning-double-sided {:front-texture ""
													 :back-texture ""
													 :scale 0.3
													 :offset-x -0.63
													 :offset-y 1.0
													 :offset-z 0.3
													 :rotation-period-ms 300.0}
		:ring-lines {:rings 3
								 :segments 16
								 :radius-start 0.4
								 :radius-step 0.5
								 :y 0.02
								 :color-a [150 100 255]
								 :color-b [120 80 200]}
		:polyline-arc {:segments 20
									 :length 20.0
									 :wiggle-amp 0.1
									 :wiggle-freq 7.0
									 :color-a [110 190 255]
									 :color-b [200 230 255]}
		:wire-box {:half-size 0.5
							 :alpha 200
							 :color-a [255 255 255]
							 :color-b [255 80 80]}
		:ray-composite {:length 15.0
										:inner-width 0.17
										:outer-width 0.22
										:glow-width 1.5
										:blend-in-ms 200.0
										:blend-out-ms 700.0
										:start-color 0xD8F8D8
										:end-color 0x6AF26A}
		:ray-composite-lite {:length 15.0
												 :inner-width 0.17
												 :outer-width 0.22
												 :glow-width 1.5
												 :blend-in-ms 200.0
												 :blend-out-ms 700.0
												 :start-color 0xD8F8D8
												 :end-color 0x6AF26A}
		{}))

(defn profile-defaults
	[kind]
	{:version 1
	 :enabled? true
	 :state {:depth-test :default
					 :blend :alpha
					 :cull :default
					 :layer (if (#{:billboard-cross :animated-billboard :spinning-double-sided
									 :ray-composite :ray-composite-lite} kind)
										:translucent
										:lines)}
	 :anim {}
	 :params (kind-default-params kind)
	 :budget {}})

(defn normalize-profile
	[profile]
	(let [kind (:kind profile)
				defaults (profile-defaults kind)]
		(-> defaults
				(merge profile)
				(update :state #(merge (:state defaults) (or % {})))
				(update :anim #(or % {}))
				(update :params #(merge (:params defaults) (or % {})))
				(update :budget #(or % {})))))

(defn- assert!
	[pred message data]
	(when-not pred
		(throw (ex-info message data))))

(defn- assert-number-in-range!
	[profile-id field value min-v max-v]
	(assert! (number? value)
				 (str "ScriptRender profile parameter must be numeric: " field)
				 {:id profile-id :field field :value value})
	(assert! (<= (double min-v) (double value) (double max-v))
				 (str "ScriptRender profile parameter is out of range: " field)
				 {:id profile-id
					:field field
					:value value
					:min min-v
					:max max-v}))

(defn- validate-kind-params!
	[normalized]
	(let [pid (:id normalized)
				kind (:kind normalized)
				params (:params normalized)]
		(case kind
			:ring-lines
			(do
				(assert-number-in-range! pid :rings (get params :rings) 1 64)
				(assert-number-in-range! pid :segments (get params :segments) 3 256)
				(assert-number-in-range! pid :radius-start (get params :radius-start) 0.0 64.0)
				(assert-number-in-range! pid :radius-step (get params :radius-step) 0.0 64.0))

			:polyline-arc
			(do
				(assert-number-in-range! pid :segments (get params :segments) 2 512)
				(assert-number-in-range! pid :length (get params :length) 0.0 512.0)
				(assert-number-in-range! pid :wiggle-amp (get params :wiggle-amp) 0.0 16.0)
				(assert-number-in-range! pid :wiggle-freq (get params :wiggle-freq) 0.0 64.0))

			:wire-box
			(do
				(assert-number-in-range! pid :half-size (get params :half-size) 0.0 64.0)
				(assert-number-in-range! pid :alpha (get params :alpha) 0 255))

			:animated-billboard
			(do
				(assert! (string? (get params :texture-prefix))
								 "Animated billboard texture-prefix must be a string"
								 {:id pid :field :texture-prefix
								  :value (get params :texture-prefix)})
				(assert-number-in-range! pid :frame-count (get params :frame-count) 1 1024)
				(assert-number-in-range! pid :frame-ms (get params :frame-ms) 1.0 60000.0)
				(assert-number-in-range! pid :half-size (get params :half-size) 0.0 64.0))

			:spinning-double-sided
			(do
				(assert! (string? (get params :front-texture))
								 "Spinning double-sided front-texture must be a string"
								 {:id pid :field :front-texture
								  :value (get params :front-texture)})
				(assert! (string? (get params :back-texture))
								 "Spinning double-sided back-texture must be a string"
								 {:id pid :field :back-texture
								  :value (get params :back-texture)})
				(assert-number-in-range! pid :scale (get params :scale) 0.0 64.0)
				(assert-number-in-range! pid :rotation-period-ms
														 (get params :rotation-period-ms)
														 1.0 60000.0))

			(:ray-composite :ray-composite-lite)
			(do
				(assert-number-in-range! pid :length (get params :length) 0.0 1024.0)
				(assert-number-in-range! pid :inner-width (get params :inner-width) 0.0 64.0)
				(assert-number-in-range! pid :outer-width (get params :outer-width) 0.0 64.0)
				(assert-number-in-range! pid :glow-width (get params :glow-width) 0.0 256.0)
				(assert-number-in-range! pid :blend-in-ms (get params :blend-in-ms) 0.0 60000.0)
				(assert-number-in-range! pid :blend-out-ms (get params :blend-out-ms) 0.0 60000.0))

			nil)
		normalized))

(defn validate-profile!
	[profile]
	(assert! (map? profile)
					 "ScriptRender profile must be a map"
					 {:profile profile})
	(assert! (string? (:id profile))
					 "ScriptRender profile :id must be a string"
					 {:id (:id profile)})
	(assert! (keyword? (:kind profile))
					 "ScriptRender profile :kind must be a keyword"
					 {:kind (:kind profile)})
	(assert! (contains? (supported-kinds) (:kind profile))
					 "ScriptRender profile :kind is not supported"
					 {:kind (:kind profile)
						:supported (supported-kinds)})

	(let [unknown-top (set/difference (set (keys profile)) allowed-top-level-keys)]
		(assert! (empty? unknown-top)
						 "ScriptRender profile has unsupported top-level keys"
						 {:id (:id profile)
							:unknown-keys unknown-top}))

	(let [normalized (normalize-profile profile)
				state (:state normalized)
				unknown-state (set/difference (set (keys state)) allowed-state-keys)]
		(assert! (empty? unknown-state)
						 "ScriptRender profile has unsupported :state keys"
						 {:id (:id normalized)
							:unknown-keys unknown-state})
		(assert! (contains? allowed-depth-test (:depth-test state))
						 "ScriptRender profile :state/:depth-test is invalid"
						 {:id (:id normalized)
							:value (:depth-test state)
							:allowed allowed-depth-test})
		(assert! (contains? allowed-blend (:blend state))
						 "ScriptRender profile :state/:blend is invalid"
						 {:id (:id normalized)
							:value (:blend state)
							:allowed allowed-blend})
		(assert! (contains? allowed-cull (:cull state))
						 "ScriptRender profile :state/:cull is invalid"
						 {:id (:id normalized)
							:value (:cull state)
							:allowed allowed-cull})
		(assert! (contains? allowed-layer (:layer state))
						 "ScriptRender profile :state/:layer is invalid"
						 {:id (:id normalized)
							:value (:layer state)
							:allowed allowed-layer})
		(validate-kind-params! normalized)))
