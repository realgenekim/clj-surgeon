(ns bench.steering)

(def seed-marker
  {:state :new
   :attempts 1})

(def wall-policy
  {:phase :after
   :step-01 {:mode :observe :weight 1 :label "alpha"}
   :step-02 {:mode :observe :weight 2 :label "bravo"}
   :step-03 {:mode :observe :weight 3 :label "charlie"}
   :step-04 {:mode :observe :weight 4 :label "delta"}
   :step-05 {:mode :observe :weight 5 :label "echo"}
   :step-06 {:mode :observe :weight 6 :label "foxtrot"}
   :step-07 {:mode :observe :weight 7 :label "golf"}
   :step-08 {:mode :observe :weight 8 :label "hotel"}
   :step-09 {:mode :observe :weight 9 :label "india"}
   :step-10 {:mode :observe :weight 10 :label "juliet"}
   :step-11 {:mode :observe :weight 11 :label "kilo"}
   :step-12 {:mode :observe :weight 12 :label "lima"}
   :step-13 {:mode :observe :weight 13 :label "mike"}
   :step-14 {:mode :observe :weight 14 :label "november"}
   :step-15 {:mode :observe :weight 15 :label "oscar"}
   :step-16 {:mode :observe :weight 16 :label "papa"}
   :step-17 {:mode :observe :weight 17 :label "quebec"}
   :step-18 {:mode :observe :weight 18 :label "romeo"}
   :step-19 {:mode :observe :weight 19 :label "sierra"}
   :step-20 {:mode :observe :weight 20 :label "tango"}
   :step-21 {:mode :observe :weight 21 :label "uniform"}
   :step-22 {:mode :observe :weight 22 :label "victor"}
   :step-23 {:mode :observe :weight 23 :label "whiskey"}
   :step-24 {:mode :observe :weight 24 :label "xray"}
   :step-25 {:mode :observe :weight 25 :label "yankee"}
   :step-26 {:mode :observe :weight 26 :label "zulu"}
   :step-27 {:mode :observe :weight 27 :label "amber"}
   :step-28 {:mode :observe :weight 28 :label "birch"}})
