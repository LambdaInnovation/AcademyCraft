(ns cn.li.neoforge1211.integration.bootstrap
	(:import [cn.li.neoforge1211.shim ForgeBootstrapHelper]))

(defn create-stone-properties
	[]
	(ForgeBootstrapHelper/createStoneProperties))

(defn create-block-properties
	[^String material hardness resistance requires-correct-tool]
	(ForgeBootstrapHelper/createBlockProperties material (float hardness) (float resistance) (boolean requires-correct-tool)))

(defn carrier-block-properties
	[base]
	(ForgeBootstrapHelper/carrierBlockProperties base))

(defn create-blocks-register
	[^String mod-id]
	(ForgeBootstrapHelper/createBlocksRegister mod-id))

(defn create-items-register
	[^String mod-id]
	(ForgeBootstrapHelper/createItemsRegister mod-id))

(defn create-creative-tabs-register
	[^String mod-id]
	(ForgeBootstrapHelper/createCreativeTabsRegister mod-id))

(defn create-block-entity-types-register
	[^String mod-id]
	(ForgeBootstrapHelper/createBlockEntityTypesRegister mod-id))

(defn create-menus-register
	[^String mod-id]
	(ForgeBootstrapHelper/createMenusRegister mod-id))

(defn create-fluid-types-register
	[^String mod-id]
	(ForgeBootstrapHelper/createFluidTypesRegister mod-id))

(defn create-fluids-register
	[^String mod-id]
	(ForgeBootstrapHelper/createFluidsRegister mod-id))

(defn create-sounds-register
	[^String mod-id]
	(ForgeBootstrapHelper/createSoundsRegister mod-id))

(defn create-effects-register
	[^String mod-id]
	(ForgeBootstrapHelper/createEffectsRegister mod-id))

(defn create-particle-types-register
	[^String mod-id]
	(ForgeBootstrapHelper/createParticleTypesRegister mod-id))

(defn create-carrier-scripted-dynamic-block
	[^String block-id ^String tile-id properties block-properties]
	(ForgeBootstrapHelper/createCarrierScriptedDynamicBlock block-id tile-id properties block-properties))

(defn create-dynamic-state-block
	[^String block-id properties block-properties]
	(ForgeBootstrapHelper/createDynamicStateBlock block-id properties block-properties))

(defn create-carrier-scripted-block
	[^String block-id ^String tile-id block-properties]
	(ForgeBootstrapHelper/createCarrierScriptedBlock block-id tile-id block-properties))

(defn create-plain-block
	[block-properties]
	(ForgeBootstrapHelper/createPlainBlock block-properties))

(defn create-fluid-type
	[luminosity density viscosity temperature can-hydrate supports-boating
	 still-texture flowing-texture overlay-texture tint-color]
	(ForgeBootstrapHelper/createFluidType
		(int luminosity)
		(int density)
		(int viscosity)
		(int temperature)
		(boolean can-hydrate)
		(boolean supports-boating)
		^String still-texture
		^String flowing-texture
		(when overlay-texture ^String overlay-texture)
		(unchecked-int tint-color)))


(defn create-flowing-fluid-properties
	[fluid-type-supplier source-supplier flowing-supplier bucket-supplier block-supplier
	 slope-find-distance level-decrease-per-block tick-rate explosion-resistance can-convert-to-source]
	(ForgeBootstrapHelper/createFlowingFluidProperties
		fluid-type-supplier source-supplier flowing-supplier bucket-supplier block-supplier
		(int slope-find-distance)
		(int level-decrease-per-block)
		(int tick-rate)
		(float explosion-resistance)
		(boolean can-convert-to-source)))

(defn create-source-fluid
	[properties]
	(ForgeBootstrapHelper/createSourceFluid properties))

(defn create-flowing-fluid
	[properties]
	(ForgeBootstrapHelper/createFlowingFluid properties))

(defn create-liquid-block
	[fluid-supplier light-level]
	(ForgeBootstrapHelper/createLiquidBlock fluid-supplier (int light-level)))

(defn create-scripted-liquid-block
	[fluid-supplier block-id tile-id light-level]
	(ForgeBootstrapHelper/createScriptedLiquidBlock
		fluid-supplier ^String block-id ^String tile-id (int light-level)))

(defn create-fluid-bucket
	[fluid-supplier]
	(ForgeBootstrapHelper/createFluidBucket fluid-supplier))

(defn create-entity-type
	[^String full-id ^Class entity-class ^String category width height
	 client-tracking-range update-interval fire-immune?]
	(ForgeBootstrapHelper/createEntityType
		full-id
		entity-class
		category
		(float width)
		(float height)
		(int client-tracking-range)
		(int update-interval)
		(boolean fire-immune?)))

(defn create-entity-type-by-kind
	[^String full-id ^String entity-kind ^String category width height
	 client-tracking-range update-interval fire-immune?]
	(ForgeBootstrapHelper/createEntityTypeByKind
		full-id
		entity-kind
		category
		(float width)
		(float height)
		(int client-tracking-range)
		(int update-interval)
		(boolean fire-immune?)))

(defn create-scripted-block-entity-type
	[^String tile-id blocks block-id-resolver]
	(ForgeBootstrapHelper/createScriptedBlockEntityType tile-id blocks block-id-resolver))

(defn find-block
	[^String namespace ^String path]
	(ForgeBootstrapHelper/findBlock namespace path))

(defn get-air-block
	[]
	(ForgeBootstrapHelper/getAirBlock))

(defn air-block?
	[block air-block]
	(ForgeBootstrapHelper/isAirBlock block air-block))
