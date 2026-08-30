(ns splice-reference.fixture)

(def alpha-forms
  [{:slot :alpha-a :form '(def alpha "alpha") :pair [:alpha :alphabet] :literal "alpha" :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :alpha-b :form '(def alpha "alphabet") :pair [:alphabet :alpha] :literal "alpha" :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? true}
   {:slot :alpha-c :form '(def alphabet "alpha") :pair [:alpha :alphabet] :literal "alphabet" :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :alpha-d :form '(def alpha-prefix "alpha") :pair [:alphabet :alpha] :literal "alphabet" :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}])

(def keyword-twins
  [{:slot :pair-a :primary [:alpha :alphabet] :secondary [:alphabet :alpha] :position :first :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :pair-b :primary [:alphabet :alpha] :secondary [:alpha :alphabet] :position :second :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :pair-c :primary [:alpha :alphabet] :secondary [:alphabet :alpha] :position :second :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :pair-d :primary [:alphabet :alpha] :secondary [:alpha :alphabet] :position :first :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? true}])

(def substring-siblings
  [{:slot :text-a :needle "alpha" :sibling "alphabet" :rendered "(def alpha \"alpha\")" :position :short :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? true}
   {:slot :text-b :needle "alphabet" :sibling "alpha" :rendered "(def alpha \"alphabet\")" :position :long :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :text-c :needle "alpha" :sibling "alpha-prefix" :rendered "(def alphabet \"alpha\")" :position :short :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :text-d :needle "alpha-prefix" :sibling "alpha" :rendered "(def alpha-prefix \"alpha\")" :position :long :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}])

(def repeated-literals
  [{:slot :literal-a :literal "alpha" :quoted '(def alpha "alpha") :pair [:alpha :alpha] :position :north :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :literal-b :literal "alpha" :quoted '(def alpha "alpha") :pair [:alpha :alpha] :position :south :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}
   {:slot :literal-c :literal "alpha" :quoted '(def alpha "alpha") :pair [:alpha :alpha] :position :east :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? true}
   {:slot :literal-d :literal "alpha" :quoted '(def alpha "alpha") :pair [:alpha :alpha] :position :west :evidence [:alpha :alphabet :alpha-prefix :alpha :alphabet :alpha-prefix :stable :reference :identity :adversarial :twin :candidate] :enabled? false}])
