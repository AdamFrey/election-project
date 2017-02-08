(ns elections.data
  (:require [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [semantic-csv.core :as scsv]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]))

(def precinct-results (atom {}))

(defn result-report [results]
  (try
    (let [results     (map (fn [res]
                             (update res :total-votes #(-> % (or "0") (read-string))))
                        results)
          total-votes (reduce + (map :total-votes results))
          dem-votes   (as-> results $
                        (filter #(= (:party %) "DEM") $)
                        (first $)
                        (:total-votes $))
          rep-votes   (as-> results $
                        (filter #(= (:party %) "REP") $)
                        (first $)
                        (:total-votes $))]
      (when (< 0 total-votes)
        {:dem-votes dem-votes
         :rep-votes rep-votes
         :dem-percentage-lead (- (float (/ dem-votes total-votes))
                                 (float (/ rep-votes total-votes)))}))
    (catch Exception ex
      (prn ex)
      (prn "-------->" results))))

(defn load-nc-2012-file []
  (reset! precinct-results {})
  (with-open [in-file (-> "data/NC/2012.txt"
                        (io/resource)
                        (io/file)
                        (io/reader))]
    (let [results (as-> in-file $
                    (csv/parse-csv $)
                    (scsv/mappify $)
                    (filter #(= (:county %) "WAKE") $)
                    (filter #(= (:contest %) "PRESIDENT AND VICE PRESIDENT OF THE UNITED STATES") $)
                    (filter #(seq (:party %)) $)
                    (map #(rename-keys % {(keyword "total votes") :total-votes}) $)
                    (map #(select-keys % [:party :precinct :total-votes]) $)
                    (group-by :precinct $))]
      (doseq [[precinct results] results]
        (swap! precinct-results
          (fn [acc]
            (update acc precinct assoc "2012" (result-report results))))))
    nil))

(defn load-nc-2016-file []
  (with-open [in-file (-> "data/NC/2016.txt"
                        (io/resource)
                        (io/file)
                        (io/reader))]
    (let [results (as-> in-file $
                    (csv/parse-csv $ :delimiter \tab)
                    (scsv/mappify $)
                    (filter #(= (:County %) "WAKE") $)
                    (filter #(= ((keyword "Contest Name") %) "US PRESIDENT") $)
                    (map #(rename-keys % {(keyword "Total Votes")  :total-votes
                                          (keyword "Contest Name") :contest
                                          :Precinct                :precinct
                                          (keyword "Choice Party") :party}) $)
                    (filter #(seq (:party %)) $)
                    (map #(select-keys % [:party :precinct :total-votes]) $)
                    (group-by :precinct $))]
      (doseq [[precinct results] results]
        (swap! precinct-results
          (fn [acc]
            (update acc precinct assoc "2016" (result-report results))))))
    nil))

(defn make-results []
  (do
    (load-nc-2012-file)
    (load-nc-2016-file)
    (->> @precinct-results
      (json/write-str)
      (spit "resources/data/NC/wake-precinct-results.json"))))

(comment
  (make-results)
  )
