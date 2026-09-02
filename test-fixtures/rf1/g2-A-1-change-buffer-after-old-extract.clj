(ns clj-surgeon.mcp-change-buffer
  "Proof-carrying semantic selection followed by one addressed transaction."
  (:require
   [clj-surgeon.diagnostic-delta :as diagnostic-delta]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-exact-verify :as mcp-exact-verify :refer [admission-unverified? expand-command run-process!]]
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.zip :as z])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file LinkOption Path Paths)
   (java.security MessageDigest)
   (java.util UUID)))
