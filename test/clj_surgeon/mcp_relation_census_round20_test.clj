(ns clj-surgeon.mcp-relation-census-round20-test
  "Round-twenty witnesses for the relation-census lane.

   A namespace of its own, and the reason is a gate rather than taste: the
   trunk's `default-ceilings-admit-every-source-in-this-repository` requires
   the shipped parser ceiling to keep a 4x margin over the largest source in
   this repository, and `mcp_relation_census_test.clj` was ALREADY over that
   line at 563c300d — 50,214 nodes against a 50,000 budget — before round
   twenty added anything to it. Round twenty's witnesses therefore live here,
   and so should round twenty-one's. The subprocess-driven launcher witnesses
   moved to `mcp-relation-census-launcher-test` for the same reason."
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.relation-census :as census]))

