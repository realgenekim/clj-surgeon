(ns clj-surgeon.mission-forms-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-forms :as forms]
   [clojure.test :refer [deftest is testing]]))

(def old-form "(defn- field\n  [x]\n  (get x :value))")
(def basis
  {:sources {"src/a.clj" (str "; protected\n" old-form "\n(def untouched 3)\n")}
   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
             :start 12 :end (+ 12 (count old-form))}]
   :budget {:max-files 1 :max-changed-chars 1000}})
(def replacement {:file "src/a.clj" :owner "field"
                  :form "(defn- finding-field [x] (get x :value))"})

(deftest replacement-needs-no-old-whitespace
  (let [r (forms/compile-forms basis [replacement])]
    (is (:ok r))
    (is (= (str "; protected\n" (:form replacement) "\n(def untouched 3)\n")
           (get-in r [:future-sources "src/a.clj"])))
    (is (false? (:mutation-attempted r)))))

(deftest forms-refuse-before-mutation
  (doseq [[label b candidates] [["wrong owner" basis [(assoc replacement :owner "untouched")]]
                                ["wrong resulting name" basis [(assoc replacement :form "(defn other [] 1)")]]
                                ["multiple forms" basis [(assoc replacement :form "(defn- finding-field [] 1) (spit \"oops\" 1)")]]
                                ["duplicate owner" basis [replacement replacement]]
                                ["extra authority" basis [(assoc replacement :before old-form)]]
                                ["parse error" basis [(assoc replacement :form "(defn-")]]
                                ["reader eval" basis [(assoc replacement :form "#=(spit \"oops\" 1)")]]
                                ["visibility change" basis [(assoc replacement :form "(defn finding-field [x] (get x :value))")]]
                                ["invalid basis" nil [replacement]]]]
    (testing label
      (let [r (forms/compile-forms b candidates)]
        (is (false? (:ok r)))
        (is (false? (:mutation-attempted r)))))))

(deftest protected-owner-syntax-refuses
  (doseq [s ["(defn- field [] ; keep me\n 1)" "(defn- ^:private field [] 1)" "(defn- field [] #_discard 1)"]]
    (let [b (assoc basis :sources {"src/a.clj" s}
                   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                             :start 0 :end (count s)}])]
      (is (= :forms-protected-syntax (:error-type (forms/compile-forms b [replacement])))))))

(deftest audit-protected-syntax-regressions
  (doseq [source ["(defn- field [] #=(+ 1 2))"
                  "(defn- field {:private true} [] 1)"
                  "(defn- field ([x] x) {:private true})"]]
    (let [b (assoc basis :sources {"src/a.clj" source}
                   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                             :start 0 :end (count source)}])]
      (is (= :forms-protected-syntax (:error-type (forms/compile-forms b [replacement]))))))
  (doseq [form ["(defn- finding-field [] #=(+ 1 2))"
                "(defn- finding-field {:macro true} [] 1)"
                "(defn- finding-field ([x] x) {:private true})"]]
    (is (= :forms-protected-syntax
           (:error-type (forms/compile-forms basis [(assoc replacement :form form)])))))
  (let [source "(defn- field \"keep docs\" [x] (get x :value))"
        b (assoc basis :sources {"src/a.clj" source}
                 :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                           :start 0 :end (count source)}])]
    (is (= :forms-owner-mismatch (:error-type (forms/compile-forms b [replacement]))))))
