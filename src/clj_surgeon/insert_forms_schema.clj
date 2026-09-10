(ns clj-surgeon.insert-forms-schema)

(defn object [properties required]
  {:type "object" :additionalProperties false :properties properties :required required})
(def string-field {:type "string"})
(def positive {:type "integer" :minimum 1})
(def one {:type "integer" :const 1})
(def digest {:type "string" :pattern "^[0-9a-f]{64}$"})
(def owner (object {"kind" {:type "string" :enum ["def" "defn" "defn-" "deftest" "ns"]}
                    "name" {:type "string" :minLength 1}} ["kind" "name"]))
(def boundary
  {:oneOf [(object {"position" {:type "string" :enum ["first" "last"]}} ["position"])
           (object {"position" {:const "after-child"} "child" positive} ["position" "child"])]})
(def anchor
  {:oneOf [(object {"scope" {:const "top-level"} "owner" owner "expect" one
                    "position" {:type "string" :enum ["before" "after"]}}
                   ["scope" "owner" "expect" "position"])
           (object {"scope" {:const "body"} "owner" owner "expect" one
                    "arity" positive "boundary" boundary
                    "testing_path" {:type "array" :items (object {"label" string-field "expect" one} ["label" "expect"])}}
                   ["scope" "owner" "expect" "boundary"])]})
(def read-receipt
  (object {"version" one "read_complete" {:const true} "workspace_root" string-field
           "file" string-field "sha256" digest}
          ["version" "read_complete" "workspace_root" "file" "sha256"]))
(def schema
  (object {"version" one "workspace_root" string-field "file" string-field
           "guard" {:oneOf [(object {"sha256" digest} ["sha256"])
                            (object {"read_receipt" read-receipt} ["read_receipt"])]}
           "anchor" anchor
           "payload" (object {"text" {:type "string" :maxLength 1048576}
                              "forms" (assoc positive :maximum 1000)} ["text" "forms"])}
          ["version" "workspace_root" "file" "guard" "anchor" "payload"]))
