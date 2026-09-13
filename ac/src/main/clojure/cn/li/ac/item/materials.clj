(ns cn.li.ac.item.materials
  "AcademyCraft material item declarations used by chip/crystal progression."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.util.log :as log]))

(def ^:private material-specs
  [{:id "imag_silicon_ingot"
    :display-name "虚像硅锭"
    :tooltip ["由虚像硅矿精炼得到"
              "用于制作晶圆和芯片"]}
   {:id "imag_silicon_piece"
    :display-name "硅晶片"
    :tooltip ["由晶圆切割得到"
              "数据芯片的基础材料"]}
   {:id "data_chip"
    :display-name "数据芯片"
    :tooltip ["基础数据处理芯片"
              "用于升级为计算芯片"]}
   {:id "calc_chip"
    :display-name "计算芯片"
    :tooltip ["高阶运算芯片"
              "用于机器和矩阵设备"]}
   {:id "constraint_ingot"
    :display-name "束能金属锭"
    :tooltip ["由束能矿提炼得到"
              "束能板的原材料"]}
   {:id "crystal_low"
    :display-name "低纯度虚能水晶"
    :tooltip ["基础虚能晶体"
              "可在 Imag Fusor 中提纯"]}
   {:id "crystal_normal"
    :display-name "中纯度虚能水晶"
    :tooltip ["经一次提纯后的晶体"
              "可继续提纯"]}
   {:id "crystal_pure"
    :display-name "高纯度虚能水晶"
    :tooltip ["高阶设备使用的纯净晶体"]}])

(defn init-materials!
  []
  (install/framework-once! ::materials-installed?
  (fn []
    (doseq [{:keys [id display-name tooltip]} material-specs]
      (idsl/register-item!
        (idsl/create-item-spec
          id
          {:max-stack-size 64
           :creative-tab :misc
           :properties {:display-name display-name
                        :tooltip tooltip
                        :model-texture id}})))
    (log/debug "Material items initialized:" (mapv #(get % :id) material-specs)))))
