#!/usr/bin/env bb
;; Emits the GitHub Actions matrix for ONE test lane, read at workflow time
;; from `clj-surgeon.lane-manifest` -- the same map the runner obeys.
;;
;; The point of reading the manifest instead of listing namespaces in the YAML
;; is TEST-ISO-001's whole argument: which namespace is in which lane is
;; declared in exactly one place. A hand-maintained matrix in the workflow
;; would be a second authority, and the failure mode is silent -- a new
;; battery namespace would simply never run in CI while the suite stayed
;; green, which is the `mcp-formatter-test` orphan all over again, one layer up.
;;
;; Usage: bb --classpath test .github/scripts/lane_matrix.clj battery
;;        bb --classpath test .github/scripts/lane_matrix.clj battery --self-test

(require '[clj-surgeon.lane-manifest :as lm]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(def apt-for
  "Namespace -> apt packages that cell needs on a bare ubuntu runner. Read
   from round one's runtime sampler (2026-09-04-suite-spike-round1.md), not
   guessed: the sampler is the only thing that saw these children, because no
   source scan of the namespace names the binary."
  {"clj-surgeon.mcp-relation-census-test" "strace"})

(def kondo-for
  "Namespaces that shell out to clj-kondo. admit-patch-test runs it 18 times."
  #{"clj-surgeon.admit-patch-test"})

(defn cell
  [ns-sym]
  (let [n (str ns-sym)]
    {:ns n
     :short (str/replace n #"^clj-surgeon\." "")
     :cadence (str (lm/cadence-of ns-sym))
     :apt (get apt-for n "")
     :kondo (str (contains? kondo-for n))}))

(defn matrix
  [lane]
  (let [nss (lm/namespaces-for lane)]
    (when (empty? nss)
      (binding [*out* *err*]
        (println (format (str "matrix-refused: lane %s resolves to zero namespaces in "
                              "clj-surgeon.lane-manifest. A zero-wide matrix is a job that "
                              "reports success having run nothing; known lanes are %s.")
                         lane (lm/lane-catalogue))))
      (System/exit 1))
    (mapv cell nss)))

(defn -self-test
  []
  (let [b (matrix :battery)
        f (matrix :fast)]
    (assert (= 11 (count b)) (str "expected 11 battery namespaces, got " (count b)))
    (assert (some #(= "strace" (:apt %)) b) "the strace cell vanished from the battery lane")
    (assert (some #(= "true" (:kondo %)) b) "the clj-kondo cell vanished from the battery lane")
    (assert (every? #(= ":landing-and-nightly" (:cadence %)) b)
            "a battery cell no longer declares the landing-and-nightly cadence")
    (assert (seq f) "the fast lane is empty")
    (println "lane_matrix self-test OK:" (count b) "battery cells," (count f) "fast")))

(let [[lane flag] *command-line-args*]
  (if (= "--self-test" flag)
    (-self-test)
    (println (json/generate-string (matrix (keyword (or lane "battery")))))))
