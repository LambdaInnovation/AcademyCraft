# Tile DSL锛坄deftile` / `deftile-kind`锛変娇鐢ㄦ寚鍗?
鏈」鐩殑 BlockEntity锛堟棫绉?tile entity锛夋敞鍐岄噰鐢?**鍏冩暟鎹┍鍔?*锛歍ile DSL 涓庡厓鏁版嵁鍦?**`mcmod`** 澹版槑锛屽叿浣撴柟鍧楀唴瀹瑰湪 **`ac`**锛汧orge 閫傞厤灞傞亶鍘嗗厓鏁版嵁骞舵敞鍐岋紝閬垮厤鍦ㄥ钩鍙板眰纭紪鐮佸唴瀹瑰垪琛ㄣ€傦紙Fabric 瀛愬伐绋嬪綋鍓嶅凡绾冲叆鏍规瀯寤猴紝鎸?minimal maintenance 缁存姢銆傦級

涓?**`defblock`**銆丗orge **`register-block-entities!`** 鐨勮鎺ヨ **`docs/02-architecture/Runtime_And_DSL_CN.md`**銆?
鐩稿叧浠ｇ爜浣嶇疆锛?- `mcmod/src/main/clojure/cn/li/mcmod/block/tile_dsl.clj`锛歍ile DSL锛坄deftile`銆乣deftile-kind`锛?- `mcmod/src/main/clojure/cn/li/mcmod/protocol/metadata.clj`锛氬钩鍙颁晶鏌ヨ鍏ュ彛锛坱ile-id / block->tile 鏄犲皠锛?
---

## 1. `deftile-kind`锛氬鐢ㄤ竴濂楃敓鍛藉懆鏈熼€昏緫

褰撳涓?tile 鍏变韩鍚屼竴濂?tick/NBT 閫昏緫鏃讹紝浼樺厛鐢?`deftile-kind` 娉ㄥ唽榛樿閫昏緫锛岀劧鍚庡湪 `deftile` 涓彧濉啓 `:tile-kind` 鍗冲彲锛屽噺灏戞牱鏉夸唬鐮併€?
```clojure
(ns cn.li.ac.block.my-machine
  (:require [cn.li.mcmod.block.tile-dsl :as tdsl]))

(defn my-tick [level pos state be] ...)
(defn my-read [tag] ...)
(defn my-write [be tag] ...)

(tdsl/deftile-kind :my-machine
  :tick-fn my-tick
  :read-nbt-fn my-read
  :write-nbt-fn my-write)
```

---

## 2. `deftile`锛氬０鏄?tile 鍏冩暟鎹紙BlockEntityType锛?
`deftile` 鐨勬牳蹇冧俊鎭細
- `:id`锛歵ile-id锛堝缓璁?kebab-case锛屼綔涓哄厓鏁版嵁涓婚敭锛?- `:registry-name`锛氭敞鍐屽悕锛坰nake_case锛夈€?*瀵瑰凡鏈変笘鐣?瀛樻。瑕佷繚鎸佺ǔ瀹?*銆?- `:blocks`锛氱粦瀹氬埌鍝簺 block-id锛堝彲 1 涓垨澶氫釜锛?- `:tile-kind`锛氬鐢?`deftile-kind` 娉ㄥ唽鐨勯粯璁ら€昏緫锛堝彲閫夛級
- `:tick-fn` / `:read-nbt-fn` / `:write-nbt-fn`锛氱洿鎺ュ啓鍦?tile 涓婏紙鍙€夛紝浼樺厛绾ч珮浜?kind 榛樿锛?
```clojure
(tdsl/deftile my-machine-tile
  :id "my-machine"
  :impl :scripted
  :registry-name "my_machine"     ;; 绋冲畾娉ㄥ唽鍚嶏紙閲嶈锛?  :blocks ["my-machine"]          ;; 缁戝畾 1 涓?block
  :tile-kind :my-machine)         ;; 澶嶇敤 kind 榛樿閫昏緫
```

---

## 3. 涓€涓?tile 缁戝畾澶氫釜 blocks锛堝噺灏?BlockEntityType 鏁伴噺锛?
瀵逛簬鏂板唴瀹癸紙灏氭湭鍙戝竷/涓嶆媴蹇冩棫涓栫晫鍏煎锛夊彲浠ヨ涓€涓?tile 缁戝畾澶氫釜 blocks锛?
```clojure
(tdsl/deftile my-tiers-tile
  :id "my-tiers"
  :impl :scripted
  :registry-name "my_tiers"
  :blocks ["my-tier-1" "my-tier-2" "my-tier-3"]
  :tile-kind :my-machine)
```

娉ㄦ剰锛?- 杩欎細璁╁涓?blocks 鍏变韩鍚屼竴涓?BlockEntityType锛堝钩鍙颁晶浼氭寜 tile-id 娉ㄥ唽锛夈€?- 濡傛灉杩欎簺 blocks 鍦ㄦ棫鐗堟湰閲屾浘缁忓悇鑷嫢鏈変笉鍚岀殑 BlockEntityType 娉ㄥ唽鍚嶏紝**鍚堝苟浼氭敼鍙?BE type id**锛屽彲鑳藉鑷存棫涓栫晫鏃犳硶璇诲彇瀵瑰簲鏂瑰潡瀹炰綋銆傚宸插彂甯冨唴瀹硅璋ㄦ厧杩佺Щ銆?
---

## 4. 骞冲彴渚у浣曟秷璐癸紙浣犱笉闇€瑕佹敼骞冲彴浠ｇ爜锛?
Forge/Fabric 閫傞厤灞備細閫氳繃 `cn.li.mcmod.protocol.metadata` 鏌ヨ锛?- `get-all-tile-ids`
- `get-tile-registry-name`
- `get-tile-block-ids`
- `get-tile-spec`
- `get-block-tile-id`锛坆lock-id -> tile-id锛?
鍥犳鏂板 tile 鍐呭閫氬父鍙渶瑕佸湪鍐呭灞傛柊澧?`deftile`锛堜互鍙婂彲閫夌殑 `deftile-kind`锛夛紝骞冲彴渚ф敞鍐屽惊鐜棤闇€淇敼銆?
---

## 5. 瀹瑰櫒涓?capability锛堝啓鍏?tile spec锛?
杩愯鏈熶笉鍐嶄娇鐢?`tile-logic-registry` / `container-registry` / `capability-registry`銆傚鍣ㄤ笌 capability **鍙湪澹版槑鏈?*鍐欏叆 `tile-registry`锛?
| 瀛楁 | 绫诲瀷 | 璇存槑 |
|------|------|------|
| `:container` | map | 鏍囧噯閿細`:get-size` `:get-item` `:set-item!` `:remove-item` `:remove-item-no-update` `:still-valid` `:slots-for-face` `:can-place` `:can-take`锛堝吋瀹规棫閿?`:still-valid?` 绛夛紝缂栬瘧鏈熷綊涓€锛?|
| `:capability-keys` | set of keyword | 濡?`#{:wireless-receiver :fluid-handler}`锛沨andler 宸ュ巶鐢?`capability.registry/declare-capability!` 娉ㄥ唽锛岀紪璇戣繘 `ITileCapabilityLogic` |

`ac` 渚ф帹鑽愰€氳繃 `cn.li.ac.block.machine.registration/init-machine!`锛?
```clojure
(machine-reg/init-machine!
  {:tiles [{:id "my-machine" :registry-name "my_machine" :blocks ["my-machine"]
            :tick-fn my-tick :read-nbt-fn my-read :write-nbt-fn my-write}]
   :containers {"my-machine" {:get-size (fn [be] 9) :get-item ...}}
   :capabilities [{:key :wireless-receiver :interface IWirelessReceiver :factory my-factory}]
   :blocks [...]})
```

`:containers` / `:capabilities` 璇硶淇濈暀锛屽唴閮ㄥ悎骞惰繘 tile spec 鍚?`register-tile!`锛?*涓嶅啀**璋冪敤宸插垹闄ょ殑 `register-container!` / `register-capability!`銆?
---

## 6. 缂栬瘧涓庡畨瑁咃紙`platform-src/minecraft/mc-1.20.1`锛?
娉ㄥ唽鏈熷钩鍙拌皟鐢ㄥ叡浜祦姘寸嚎锛團orge/Fabric 鐩稿悓锛夛細

1. `cn.li.mc1201.block.logic-pipeline/compile-all-bundles` 鈥?閬嶅巻 `tile-dsl`锛屽悎骞?`tile-kind` 榛樿锛屼骇鍑?`TileLogicBundle`銆?2. 姣忎釜 `IScriptedBlock` 瀹炰緥鍒涘缓鍚庣珛鍗?`install-bundle-to-block!`銆?3. Forge 鍦?common setup 璋冪敤 `assert-all-blocks-have-bundle!`锛堟湭瀹夎鍒欏惎鍔ㄥけ璐ワ級銆?
鐑矾寰勶細`AbstractScriptedBlockEntity` 閫氳繃 `getBlockState().getBlock()` 璇诲彇 bundle锛屾棤 `RT.var`銆?
璇﹁ [SCRIPTED_LOGIC_DISPATCH.md](../04-systems/SCRIPTED_LOGIC_DISPATCH.md)銆?
