(ns cn.li.ac.block.imag-phase.block
  "Imaginary phase fluid block port for 1.20.

  Registers a fluid + block with an animated overlay TESR that mirrors
  the upstream RenderImagPhaseLiquid (3 scrolling layers)."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.ac.block.imag-phase.handlers :as imag-phase-handlers]
            [cn.li.ac.registry.hooks :as hooks]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.tile-dsl :as tdsl]
            [cn.li.mcmod.fluid.dsl :as fdsl]
            [cn.li.mcmod.worldgen :as mcmod-worldgen]
            [cn.li.mcmod.util.log :as log]))

(defn init-imag-phase!
  []
  (install/framework-once! ::imag-phase-installed?
  (fn []
    (fdsl/register-fluid!
      (fdsl/create-fluid-spec
        "imag-phase"
        {:registry-name "imag_phase"
         :physical {:luminosity 8
                    :density 7000
                    :viscosity 6000
                    :temperature 0
                    :can-convert-to-source false
                    :supports-boat false}
         ;; Upstream ACFluids registers `imagproj` with black still/flowing
         ;; textures: the fluid surface itself is featureless and the whole
         ;; visible effect comes from the TESR overlay. phase_liquid is the
         ;; item icon only (see the block spec's :rendering below).
         :rendering {:still-texture (modid/asset-path "block" "black")
                     :flowing-texture (modid/asset-path "block" "black")
                     :overlay-texture (modid/asset-path "block" "black")
                     :tint-color -1
                     :is-translucent true}
         ;; Upstream BlockImagPhase sets quantaPerBlock 3, so a source spreads
         ;; only 2 blocks. 1.20 fluids are fixed at 8 levels, so the equivalent
         ;; is a decrease of 3 per block (8 -> 5 -> 2): same 3-tile reach.
         ;; Flow parity with upstream: BlockFluidClassic's tickRate =
         ;; viscosity / 200 (verified in forge 1.12.2 bytecode) = 6000/200 = 30
         ;; ticks per flow step. The original port's 8 flowed ~4x too fast.
         :behavior {:slope-find-distance 3
                    :level-decrease-per-block 3
                    :tick-rate 30
                    :explosion-resistance 100.0}
         ;; No bucket — upstream ACFluids registers fluidImagProj without a
         ;; bucket (FluidRegistry.registerFluid only); the local port's
         ;; has-bucket? defaults true, so it must be turned off explicitly.
         :block {:block-id "imag-phase"
                 :registry-name "imag_phase"
                 :has-bucket? false}}))
    ;; Register a tile so has-block-entity? returns true, enabling
    ;; ScriptedLiquidBlock creation and TESR attachment.
    (tdsl/register-tile!
      (tdsl/create-tile-spec
        "imag-phase"
        {:impl :scripted
         :blocks ["imag-phase"]}))
    ;; Block spec: has-item-form? true so a BlockItem is generated. Upstream's
    ;; item model is item/generated over blocks/phase_liquid, so flat-item-icon?
    ;; is required — the default parents the item model to the cube model and
    ;; shows a 3D block in the creative tab instead of the animated flat icon.
    ;; Forge registration selects ScriptedLiquidBlock when fluid metadata
    ;; exists and has-block-entity?.
    (bdsl/register-block!
      (bdsl/create-block-spec
        "imag-phase"
        {:registry-name "imag_phase"
         :physical {:material :stone
                    :hardness 100.0
                    :resistance 100.0
                    :requires-tool false
                    :sounds :stone}
         :rendering {:model-parent "minecraft:block/cube_all"
                     :textures {:all (modid/asset-path "block" "phase_liquid")}
                     :has-item-form? true
                     :flat-item-icon? true}
         :events {:on-right-click imag-phase-handlers/handle-imag-phase-click}}))
    ;; Register as the configurable-pool fill block so worldgen providers
    ;; can resolve the fill block without hardcoding content-specific IDs.
    (mcmod-worldgen/register-pool-fill-block-id! (str modid/MOD-ID ":imag_phase"))
    ;; Attach the animated TESR overlay (3 scrolling layers)
    (hooks/register-client-renderer! 'cn.li.ac.block.imag-phase.render/init!)
    (log/info "Initialized Imag Phase fluid block with TESR"))))
