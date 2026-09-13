(ns cn.li.mcmod.test.class-gen-guard
  "铁律十三通用测试工具：断言高频调用路径零运行时动态类生成。

  若函数内部有闭包捕获外层变量（fn-inside-fn-in-loop），每次调用
  都会在 JVM Metaspace 生成新类。此工具在 warm-up 后测量 ClassLoadingMXBean，
  断言指定迭代次数后零新类产生。

  用法 1（需要 mock 平台函数）：
    (deftest my-updater-test
      (class-gen-guard/with-mocks-zero-class-gen
        \"my-updater\"
        [#'world/get-state (fn [_ _] :mock)
         #'world/set-state (fn [_ _ _] nil)]
        (dotimes [i 10000]
          (my-updater state level pos))))

  用法 2（不需要 mock，直接断言）：
    (deftest my-fn-no-class-gen-test
      (class-gen-guard/assert-zero-class-gen
        \"my-fn\"
        (fn [] (my-fn arg1 arg2))
        10000))"
  (:require [clojure.test :refer [is]]))

;; ── 核心测量原语 ──

(defn loaded-class-count
  "Return current total loaded class count from JVM ClassLoadingMXBean."
  []
  (.getLoadedClassCount
    ^java.lang.management.ClassLoadingMXBean
    (java.lang.management.ManagementFactory/getClassLoadingMXBean)))

(defmacro with-mocks-zero-class-gen
  "安装 mock（alter-var-root），warm-up 后执行 body 并断言零运行时类生成。

  label:       测试标识字符串
  bindings:    #'var mock-value 对（必须用 #' 引用 Var）
  warm-up-n:   warm-up 迭代次数（默认 100）
  measure-n:   测量期迭代次数（默认 10000）
  body:        被测循环体（会被包裹在 dotimes 中执行）"
  [label bindings & {:keys [warm-up-n measure-n body]
                     :or {warm-up-n 100 measure-n 10000}}]
  (assert (even? (count bindings)) "bindings must be #'var mock-value pairs")
  (let [pairs (partition 2 bindings)
        syms (repeatedly (count pairs) gensym)]
    `(let [~@(interleave syms (map (fn [[v mock]]
                                     `(let [orig# (var-get ~v)]
                                        (alter-var-root ~v (constantly ~mock))
                                        [~v orig#]))
                                   pairs))]
       (try
         (dotimes [_# ~warm-up-n] ~@body)
         (let [before# (loaded-class-count)]
           (dotimes [_# ~measure-n] ~@body)
           (let [after# (loaded-class-count)
                 gen#   (- after# before#)]
             (is (zero? gen#)
                 (str "铁律十三红线 [" ~label "]: "
                      ~measure-n " 次调用产生了 " gen#
                      " 个运行时类！检查闭包是否捕获了外层变量，"
                      "应提取为 defn- + partial。"))))
         (finally
           (doseq [[v# orig#] [~@syms]]
             (alter-var-root v# (constantly orig#))))))))

(defn assert-zero-class-gen
  "最简单的断言形式：对 (thunk) 执行 iterations 次，断言零新类。

  label:      测试标识
  thunk:      零参函数，封装被测调用
  iterations: 迭代次数（默认 10000）

  thunk 内部须自行处理 mock。优先用 with-mocks-zero-class-gen。"
  ([label thunk] (assert-zero-class-gen label thunk 10000))
  ([label thunk iterations]
   (dotimes [_ 100] (thunk))
   (let [before-cnt (loaded-class-count)]
     (dotimes [_ iterations] (thunk))
     (let [generated (- (loaded-class-count) before-cnt)]
       (is (zero? generated)
           (str "铁律十三红线 [" label "]: "
                iterations " 次调用产生了 " generated
                " 个运行时类！闭包须提取为 defn- + partial。"))))))
