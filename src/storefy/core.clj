(ns storefy.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str]
            [storefy.utils :refer :all]
            [storefy.visualize :refer [visualize]])
  (:import [java.time LocalDateTime LocalTime LocalDate temporal.IsoFields
            temporal.ChronoField]))

(defn -main []
  (let [url (current-storefy)
        dom (get-dom url)
        tables (parse-page dom)
        lectures (-> tables
                     (to-joda-time (get-year) (get-week))
                     (flatten-tables)
                     (restructure-table))]
    (visualize lectures)))

