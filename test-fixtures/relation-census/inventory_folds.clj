(ns inventory-lens.folds
  "A SECOND arm-defining fixture, so the pool-invariance witness runs more than
   one unit through the pool.

   The shapes here are the ordinary ones a projection carries: a door call, a
   set target, an inline membership guard, a helper-mediated guard, and one
   unguarded append. They are constructed, not copied — folds.clj carries the
   real bytes; this file exists to give the plan phase a second file to place on
   a different thread."
  (:require
   [clojure.string :as str]))

(defmulti fold-event (fn [_state event] (:type event)))

(defmethod fold-event :default [state _event] state)

(defn- conj-once
  [coll x]
  (let [coll (vec (or coll []))]
    (if (some #(= x %) coll) coll (conj coll x))))

(defn- sku-recorded?
  [state sku]
  (some #(= sku (:sku %)) (get-in state [:inventory :items])))

(defn- normalized-sku
  [sku]
  (-> (or sku "") str str/trim str/upper-case))

;; A write routed through an identity door: :door.
(defmethod fold-event "inventory.item-stocked"
  [state event]
  (let [item {:sku (normalized-sku (:sku event)) :qty (:qty event)}]
    (update-in state [:inventory :items] conj-once item)))

;; A write into a set: :set.
(defmethod fold-event "inventory.warehouse-opened"
  [state event]
  (update-in state [:inventory :warehouses] (fnil conj #{}) (:warehouse event)))

;; An inline membership guard on the written value's identity: :guarded.
(defmethod fold-event "inventory.count-adjusted"
  [state event]
  (let [adjustment {:adjustment-id (:adjustment-id event)
                    :delta (:delta event)}]
    (if (not-any? #(= (:adjustment-id adjustment) (:adjustment-id %))
                  (get-in state [:inventory :adjustments]))
      (update-in state [:inventory :adjustments] conj adjustment)
      state)))

;; The guard is a helper this census version declines to reason through:
;; :unknown, reason :helper-mediated-guard.
(defmethod fold-event "inventory.sku-registered"
  [state event]
  (if (sku-recorded? state (:sku event))
    state
    (update-in state [:inventory :items] conj {:sku (:sku event) :qty 0})))

;; No recognised guard dominates this append: :raw.
(defmethod fold-event "inventory.shipment-received"
  [state event]
  (update-in state [:inventory :shipments] (fnil conj []) (:shipment event)))
