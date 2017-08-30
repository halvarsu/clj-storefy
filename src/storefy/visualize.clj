(ns storefy.visualize
  (:gen-class)
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clj-time.core :as t]
            [storefy.utils :as utils]))


(defn setup [lectures]
  (q/color-mode :hsb)
  (q/frame-rate 5)
  {:i 0
   :lectures lectures
   :current (utils/next-or-current lectures) 
   :now (utils/local-now)})

(defn update-state [state]
  (-> state
      (update :now (utils/local-now))
      (update :current utils/if-update-lecture (:lectures state) (:now state))
      (update :i inc)))

(defn status-text [state]
  (let [lecture (:current state)
        now (:now state)
        min-left (t/in-minutes (utils/time-left lecture now))
        course (:course lecture)]
    (if (:active? lecture)
      (str course " in progress, " min-left " minutes left")
      (str min-left " minutes left until " course))))


(defn draw-state [state]
  (let [i (:i state) 
        t 3
        x 25
        y (mod i (+ (q/height) 1))] 
    (q/background 100 100 50)
    (q/text-font (q/create-font "Arial" 53))
    (q/fill 255 120 160)
    (q/text (status-text state) x y)))

(def lectures
  (-> (utils/current-storefy)
      (utils/get-dom)
      (utils/parse-page)
      (utils/to-joda-time (utils/get-year) (utils/get-week))
      (utils/flatten-tables)
      (utils/restructure-table)))

(def test-state {:i 0
   :lectures lectures
   :current (utils/next-or-current lectures) 
   :now (utils/local-now)})


(status-text test-state)

(visualize lectures)

(defn mouse [state event]
  (println (:i state)) 
  (println (:current state)) 
  (println event)
  state)

(defn visualize [lectures]
  (q/defsketch sketch
    :size :fullscreen
    :draw draw-state
    :update update-state
    :mouse-pressed mouse
    :setup (partial setup lectures)
    :features [:keep-on-top]
    :middleware [m/pause-on-error m/fun-mode]))

