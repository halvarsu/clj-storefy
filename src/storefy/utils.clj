(ns storefy.utils
  (:require [net.cgrand.enlive-html :as html]
            [quil.core :as q]
            [quil.middleware :as m]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.string :as str])
  (:import [java.time LocalDateTime LocalTime LocalDate temporal.IsoFields
            temporal.ChronoField]))

(defn download [url]
  (println "Downloading" url)
  (slurp url))

(defn download-dom [url]
  (html/html-resource (download url)))

(defn save-dom [url]
  (spit "web_page.html" (download url)))

(defn get-dom 
  ([url] (get-dom url false))
  ([url force-download]
   "Loads from disk as default, if no file then downloads"
   (let [file (java.io.File. "web_page.html")]
     (if (or (.exists file) force-download)
       (do (println "File exists, loading") 
           (html/html-resource file))
       (do (println "File doesnt exist, downloading (and saving)")
           (let [page (download url)]
             (spit "web_page.html" page)
             (html/html-resource page))))))
  )

(defn load-dom []
  (html/html-resource (java.io.File. "web_page.html")))

(defn get-url 
  [room building week year]
  (str "https://tp.uio.no/timeplan/romutskrift.php?id=" building
       "&room=" building room 
       "&week=" week 
       "&year=" year))

(defn get-year
  ([] (get-year (LocalDateTime/now)))
  ([date] (.getYear date)))

(defn get-week 
  ([] (get-week (LocalDateTime/now)))
  ([date] (.get date (IsoFields/WEEK_OF_WEEK_BASED_YEAR))))

(defn get-day-of-week
  ([] (get-day-of-week (LocalDateTime/now)))
  ([date] (.get date (ChronoField/DAY_OF_WEEK))))

(defn current-storefy []
  (let [date (LocalDateTime/now)
        year (.getYear date)
        week (get-week date)]
    (get-url "V343" "BL24" week year)))



(defn get-tables [nodes]
  (html/select nodes [:div.tp-table]))

(defn get-rows [nodes]
  (html/select nodes [:div.tp-row]))

(defn get-cells [nodes]
  (html/select nodes [:div.tp-cell]))


(defn parse-page
 [dom]
 (for [table (get-tables dom)
       :let [rows (get-rows table)
             head (get-cells (first rows))
             contents (get-cells (rest rows)) ]]
   {:head (map html/text head) 
    :content {:times (map html/text (html/select contents [:div.tid])) 
              :descriptions (map html/text (html/select contents [:div.desc])) 
              :courses (map html/text (html/select contents [:div.course])) }})) 

(def oslo-zone (t/time-zone-for-id "Europe/Oslo"))

(defn local-now []
  (t/to-time-zone 
    (t/now) oslo-zone))

(defn week-based-date-time 
  ([year week day] (week-based-date-time year week day "00" "00"))
  ([year week day hour] (week-based-date-time year week day "00"))
  ([year week day hour minute]
   (let [s (str year "-W" week "-" day "T" hour ":" minute ":00Z")]
     (t/from-time-zone (f/parse (f/formatters :week-date-time-no-ms) s)
                       oslo-zone))))


(defn split-hour-min [str-time]
  (map #(Integer. %) (str/split str-time #":")))

(defn get-clock-deltas [str-time-span]
  (-> str-time-span
      (str/split #" - ")
      (#(map split-hour-min %)))) 

(defn get-date-time-interval [year week day-of-week str-time-span]
  (let [day (week-based-date-time year week day-of-week) 
        [[start-h start-m] [stop-h stop-m]] (get-clock-deltas str-time-span)]
    (t/interval (t/plus day (t/hours start-h) (t/minutes start-m))
                (t/plus day (t/hours stop-h) (t/minutes stop-m)))))

(defn convert-table-times [table year week day-of-week]
  (assoc-in table [:content :times]
            (map #(get-date-time-interval year week day-of-week %) 
                 (get-in table [:content :times]))))


(defn to-joda-time [tables year week]
  "Expects an iterable of tables, one for each day of the week, and morphs
  the string interval on the form 'hh:mm - hh:mm' to a joda interval"
  (let [years (repeat 5 year)
        weeks (repeat 5 week)
        days (range 1 6)]
  (map convert-table-times tables years weeks days)))


(defn flatten-tables [tables]
  (apply (partial merge-with concat) (map :content tables)))

(defn restructure-table [table]
  (let [{t :times d :descriptions c :courses} table]
    (map #(hash-map :time %1 :desc %2 :course %3) t d c)))


(get-date-time-interval (get-year) (get-week) (get-day-of-week)  "12:15 - 14:00")


(defn next-or-current [lectures]
  "Given a coll of lecture-maps, returns the first one not finished, with a
  boolean tag 'active?'"
  (let [now (local-now)
        first-not-ended (first (filter #(t/before? now (.getEnd (:time %))) 
                                       lectures))
        active? (t/after? now (.getStart (:time first-not-ended)))]
    (assoc first-not-ended :active? active?)))

(defn time-left 
  ([lecture] (time-left lecture (local-now)))
  ([lecture now]
   (let [times (:time lecture)]
     (t/interval now
                 (if (:active? lecture)
                   (.getEnd times)
                   (.getStart times))))))

(defn if-update-lecture 
  ([current lectures] (if-update-lecture current lectures (local-now)))
  ([current lectures now]
   "Returns next lecture if current lecture is over"
   (if (:active? current) 
     (if (t/after? (local-now) (.getEnd (:time  current)))
       (next-or-current lectures)
       current)
     (if (t/after? (local-now) (.getStart (:time  current)))
       (assoc current :active? true)
       current))))

; TODO:
; Save data and load from file, presumably csv 
; check data md5hash to see if updated ? 
; Or maybe check html-file 

