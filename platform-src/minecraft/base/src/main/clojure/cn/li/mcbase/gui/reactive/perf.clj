(ns cn.li.mcbase.gui.reactive.perf
  "Dev performance instrumentation (Phase B acceptance requirement).
   Frame counters + render-thread allocated bytes via ThreadMXBean.
   Enabled with JVM flag -Dmcmod.ui.perf=true; zero overhead when disabled."
  (:import [java.lang.management ManagementFactory]
           [com.sun.management ThreadMXBean]))

(def enabled?
  (Boolean/getBoolean "mcmod.ui.perf"))

(def ^:private ^ThreadMXBean thread-bean
  (let [b (ManagementFactory/getThreadMXBean)]
    (when (instance? ThreadMXBean b) b)))

;; Frame counters — single mutable cell array (owner: render thread only)
(defonce ^:private ^"[J" counters (long-array 4))
;; [0]=frames [1]=last-frame-alloc-bytes [2]=alloc-baseline [3]=last-report-ms

(defn frame-start!
  "Call at the beginning of each frame to snapshot allocation baseline."
  []
  (when (and enabled? thread-bean)
    (let [^"[J" c counters]
      (aset c 2 (.getCurrentThreadAllocatedBytes thread-bean)))))

(defn frame-end!
  "Call at the end of each frame. Records per-frame allocated bytes.
   Returns a stats string every ~2s for display, else nil."
  []
  (when (and enabled? thread-bean)
    (let [^"[J" c counters
          now-alloc (.getCurrentThreadAllocatedBytes thread-bean)
          frame-alloc (- now-alloc (aget c 2))
          now-ms (System/currentTimeMillis)]
      (aset c 0 (inc (aget c 0)))
      (aset c 1 frame-alloc)
      (when (> (- now-ms (aget c 3)) 2000)
        (aset c 3 now-ms)
        (format "UI perf: frame#%d alloc=%dB" (aget c 0) frame-alloc)))))

(defn last-frame-alloc-bytes ^long []
  (let [^"[J" c counters]
    (aget c 1)))
