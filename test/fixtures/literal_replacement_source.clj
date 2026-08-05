(ns fixtures.literal-replacement-source)

(defn layout [options]
  options)

(defn page [dev-mode?]
  (layout {:dev-mode? dev-mode?}))

(defn include-current [entries current]
  (if (and current (not (some #{current} entries)))
    (cons current entries)
    entries))
