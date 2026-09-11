(ns clj-surgeon.rename-alias-schema
  (:require [clj-surgeon.insert-forms-schema :as s]))
(def nonnegative {:type "integer" :minimum 0})
(def scope
  {:oneOf [(s/object {"file" s/string-field "expect_files" s/positive} ["file" "expect_files"])
           (s/object {"paths" {:type "array" :minItems 1 :uniqueItems true :items s/string-field}
                      "expect_files" s/positive} ["paths" "expect_files"])
           (s/object {"repository" {:const true} "expect_files" s/positive} ["repository" "expect_files"])]})
(def guard (get-in s/schema [:properties "guard"]))
(def schema
  (s/object {"version" s/one "workspace_root" s/string-field "scope" scope
             "lib" s/string-field "old_alias" s/string-field "new_alias" s/string-field
             "guards" {:type "object" :additionalProperties guard}
             "expect" (s/object {"references" {:oneOf [(s/object {"total" nonnegative} ["total"])
                                                        (s/object {"per_file" {:type "object" :additionalProperties nonnegative}} ["per_file"])]}} ["references"])}
            ["version" "workspace_root" "scope" "lib" "old_alias" "new_alias" "expect" "guards"]))
