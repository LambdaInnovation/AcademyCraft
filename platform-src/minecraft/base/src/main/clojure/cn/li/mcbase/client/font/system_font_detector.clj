(ns cn.li.mcbase.client.font.system-font-detector
  "Cross-platform system TrueType font detection.

  Detects the operating system and searches known font installation paths for a
  suitable CJK-capable vector font. Returns a map with the font path, a profile
  keyword for calibration lookup, and the file extension.

  Never throws — returns nil when no font is found, so callers can fall back to
  Minecraft's default bitmap font gracefully."
  (:import [java.nio.file Path Files FileSystems]))

(def ^:private font-search-list
  "Ordered list of candidate system fonts.  Earlier entries take priority.
  Each entry maps:
    :path   — absolute filesystem path string
    :profile — keyword identifying the font family (legacy; unused by MSDF pipeline)
    :ext    — file extension (ttf or ttc)"
  [{:path "C:\\Windows\\Fonts\\msyh.ttc"
    :profile :msyh
    :ext "ttc"}
   {:path "C:\\Windows\\Fonts\\msyh.ttf"
    :profile :msyh
    :ext "ttf"}
   {:path "C:\\Windows\\Fonts\\simhei.ttf"
    :profile :simhei
    :ext "ttf"}
   {:path "C:\\Windows\\Fonts\\simsun.ttc"
    :profile :simhei
    :ext "ttc"}
   {:path "/System/Library/Fonts/PingFang.ttc"
    :profile :pingfang
    :ext "ttc"}
   {:path "/System/Library/Fonts/Helvetica.ttc"
    :profile :pingfang
    :ext "ttc"}
   {:path "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
    :profile :noto-sans
    :ext "ttc"}
   {:path "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
    :profile :noto-sans
    :ext "ttc"}
   {:path "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    :profile :dejavu
    :ext "ttf"}])

(defn detect-system-font
  "Search known system font paths and return the first match.

  Returns a map with keys:
    :path    — java.nio.file.Path to the font file
    :profile — keyword identifying the font for calibration lookup
    :ext     — String file extension (\"ttf\" or \"ttc\")

  Returns nil when no known system font is found."
  []
  (try
    (some (fn [{:keys [path profile ext]}]
            (let [^Path p (try
                      (.getPath (FileSystems/getDefault) path (into-array String []))
                      (catch Exception _ nil))]
              (when (and p
                         (try (Files/exists p (make-array java.nio.file.LinkOption 0)) (catch Exception _ false))
                         (try (Files/isReadable p) (catch Exception _ false)))
                {:path p :profile profile :ext ext})))
          font-search-list)
    (catch Exception _ nil)))
