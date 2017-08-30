(t/interval 
  (utils/local-now) 
  (.getStart (:time (last @lectures))))

  
(def next-lecture (utils/next-or-current @lectures))

(.getStart (:time next-lecture))

(utils/time-left next-lecture)

(t/in-minutes (utils/time-left next-lecture))


(def lectures
  (atom (-> (utils/current-storefy)
      (utils/get-dom )
      (utils/parse-page)
      (#(utils/to-joda-time % 2017 35))
      (utils/flatten-tables)
      (utils/restructure-table))))

(def test-time (t/interval (t/minus (utils/local-now) (t/minutes 20))

                            (t/plus (utils/local-now) (t/minutes 1))))
(def test-lecture (-> (utils/next-or-current @lectures)
                      (assoc :time test-time)
                      (assoc :active? true)))

(t/after? (utils/local-now) (.getEnd (:time test-lecture)))

(if-update-lecture test-lecture @lectures)
