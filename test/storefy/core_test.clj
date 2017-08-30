(ns storefy.core-test
  (:require [clojure.test :refer :all]
            [storefy.core :refer :all]
            [clj-time.core :as t]))

(deftest test-all-the-functions []
  (testing "split-hour-min" 
    (is (= (split-hour-min  "12:15")
             '(12 15))))
  (testing "week-based-date-time"
    (is (t/equal? (week-based-date-time  2017 5 5 5 5 )
                  (t/date-time 2017 2 3 5 5)))))
