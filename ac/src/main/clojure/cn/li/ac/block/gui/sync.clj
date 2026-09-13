(ns cn.li.ac.block.gui.sync
  "Shared GUI container sync helpers for AC block GUIs.

  This namespace keeps schema-runtime wiring out of individual block GUI files so
  simple machines can declare their schema once and reuse the same server-menu-sync
  and close behavior."
  (:require [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.schema-runtime :as schema-runtime]
            ))  ;; tabbed-gui removed — was unused

(defn create-schema-container
  "Create the common container map for schema-backed block GUIs.

  `base` is merged before schema atoms, allowing callers to provide
  `:tile-entity`, `:player`, `:container-type`, and any GUI-local values. When
  `state` is omitted, the current tile state is read with `common/get-tile-state`.

  Options:
  - `:gui-id` optional int for DataSlot budget errors
  - `:include-tab-data-slot?` default true when container will have :tab-index"
  [schema tile player container-type & [{:keys [state base gui-id include-tab-data-slot?]
                                         :or {include-tab-data-slot? true}}]]
  (let [state (or state (common/get-tile-state tile) {})
        tabbed? (or (contains? base :tab-index)
                    (some #(= :tab-index (or (:gui-container-key %) (:key %))) schema))
        field-specs (schema-runtime/build-data-slot-field-specs
                      schema
                      :gui-id gui-id
                      :include-tab? (and include-tab-data-slot? tabbed?))]
    (merge {:tile-entity tile
            :player player
            :container-type container-type
            :server-menu-sync! (schema-runtime/build-server-menu-sync! schema)
            :data-slot-field-specs field-specs}
           base
           (schema-runtime/build-gui-atoms schema state))))

(defn schema-sync-fns
  "Build reusable sync lifecycle functions for a GUI schema.

  Optional callbacks:
  - `:after-sync!` receives the container after server menu atom sync.
  - `:after-close!` receives the container after schema close resets."
  [schema & [{:keys [after-sync! after-close!]}]]
  (let [server-menu-sync* (schema-runtime/build-server-menu-sync! schema)
        on-close* (schema-runtime/build-on-close-fn schema)]
    {:server-menu-sync! (fn [container]
                          (server-menu-sync* container)
                          (when after-sync!
                            (after-sync! container)))
     :on-close (fn [container]
                 (on-close* container)
                 (when after-close!
                   (after-close! container)))}))

(defn server-menu-sync!
  "Run lightweight server menu sync when available."
  [container]
  (when-let [f (:server-menu-sync! container)]
    (f container)))
