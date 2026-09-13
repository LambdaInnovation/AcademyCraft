(ns cn.li.mcbase.runtime.server-runtime
  "Thread-confined runtime owned by one MinecraftServer instance.

  The runtime captures immutable callbacks and content hooks once, at server
  start.  Mutable counters and collections are private to the server thread;
  no atom, persistent-map rewrite, or Framework lookup occurs in the tick
  loop."
  (:import [java.util ArrayDeque HashMap Map]
           [net.minecraft.server MinecraftServer]))

(definterface IServerRuntime
  (^net.minecraft.server.MinecraftServer server [])
  (^Object sessionId [])
  (^java.util.Map callbacks [])
  (^java.util.Map hooks [])
  (^java.util.HashMap players [])
  (^java.util.HashMap worlds [])
  (^java.util.ArrayDeque dirtyPlayers [])
  (^long tickId [])
  (^boolean disposed [])
  (^void beginTick [^long next-tick])
  (^void finishTick [])
  (^void dispose []))

(deftype ServerRuntime
  [^MinecraftServer server
   session-id
   ^Map callbacks
   ^Map hooks
   ^HashMap players
   ^HashMap worlds
   ^ArrayDeque dirty-players
   ^:unsynchronized-mutable ^long tick-id
   ^:unsynchronized-mutable ^long in-tick-flag
   ^:unsynchronized-mutable ^long disposed-flag]
  IServerRuntime
  (server [_] server)
  (sessionId [_] session-id)
  (callbacks [_] callbacks)
  (hooks [_] hooks)
  (players [_] players)
  (worlds [_] worlds)
  (dirtyPlayers [_] dirty-players)
  (tickId [_] tick-id)
  (disposed [_] (not (zero? disposed-flag)))
  (beginTick [_ next-tick]
    (when-not (zero? disposed-flag)
      (throw (IllegalStateException. "Cannot tick a disposed ServerRuntime")))
    (when-not (zero? in-tick-flag)
      (throw (IllegalStateException. "ServerRuntime tick re-entry")))
    (set! tick-id (long next-tick))
    (set! in-tick-flag 1))
  (finishTick [_]
    (set! in-tick-flag 0))
  (dispose [_]
    (.clear players)
    (.clear worlds)
    (.clear dirty-players)
    (set! in-tick-flag 0)
    (set! disposed-flag 1)))

(defn create-server-runtime!
  "Create a runtime after content registration has finished.

  callbacks and hooks must already be frozen immutable maps."
  ^IServerRuntime [^MinecraftServer server callbacks hooks]
  (when-not server
    (throw (IllegalArgumentException. "create-server-runtime! requires server")))
  (when-not (and (map? callbacks) (map? hooks))
    (throw (IllegalArgumentException. "create-server-runtime! requires callback and hook maps")))
  (ServerRuntime. server
                  [:server (System/identityHashCode server)]
                  callbacks
                  hooks
                  (HashMap.)
                  (HashMap.)
                  (ArrayDeque.)
                  0
                  0
                  0))

(defn runtime-owner
  "Build the one owner value allocated for an entire server tick."
  [^IServerRuntime runtime]
  {:server-session-id (.sessionId runtime)
   :server-tick-id (.tickId runtime)})

(defn dispose-server-runtime!
  [^IServerRuntime runtime]
  (when (and runtime (not (.disposed runtime)))
    (.dispose runtime))
  nil)
