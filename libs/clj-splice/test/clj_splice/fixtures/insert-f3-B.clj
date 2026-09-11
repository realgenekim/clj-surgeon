(deftest roundtrip
  (testing "outer"
    (is 1)
    (is :new)

    (is 2)))

(defn helper [] :h)
