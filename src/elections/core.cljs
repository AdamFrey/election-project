(ns elections.core
  (:require [cljsjs.react-google-maps]
            [cljsjs.react-leaflet]
            [cljsjs.topojson]
            [clojure.string :as str]
            [elections.color :as color]
            [goog.dom :as gdom]
            [goog.object :as obj]
            [httpurr.client :as http]
            [httpurr.client.xhr :as client]
            [oops.core :as o]
            [promesa.core :as p]
            [rum.core :as rum]))

(enable-console-print!)

(def el js/React.createElement)

(def default-center (js/google.maps.LatLng. 35.7796 -78.6382))
(def default-zoom 11)

(defonce topojson-data (atom nil))
(defonce results-data (atom nil))

(defn abs [n] (max n (- n)))

(defn precinct-style [precinct]
  (let [results (get @results-data precinct)
        el-2012 (get results "2012")
        el-2016 (get results "2016")]
    (if (not (and el-2012 el-2016))
      #js {:color "gray"}

      (let [difference (- el-2016 el-2012)
            opacity    (-> difference abs (* 1000))
            params     {:strokeWeight 2}]
        (if (< difference 0)
          (-> params
            (assoc :fillColor "red" #_(color/increaseBrightness "#ff0000" opacity))
            (clj->js))
          (-> params
            (assoc :fillColor "blue" #_(color/increaseBrightness "#0000ff" opacity))
            (clj->js)))))))

(defn rum->element [rum-state]
  (some-> rum-state
    (:rum/react-component)
    (js/ReactDOM.findDOMNode)))

(defn info-window-content [precinct]
  (let [results (get @results-data precinct)
        res-2012 (-> results
                   (get "2012")
                   (* 100)
                   (.toFixed 2))
        res-2016 (-> results
                   (get "2016")
                   (* 100)
                   (.toFixed 2))]
    (str
      "<p>" precinct "</p>"
      "<p>2012 - " res-2012 "%</p>"
      "<p>2016 - " res-2016 "%</p>")))

(defn click-precinct [event map]
  (let [feature   (o/oget event "feature")
        anchor    (js/google.maps.MVCObject.)
        _         (o/ocall anchor "set" "position" (o/oget event "latLng"))
        precinct  (o/ocall feature "getProperty" "name")
        info-wind (js/google.maps.InfoWindow.
                    #js {:content (info-window-content precinct)})]
    (o/ocall info-wind "open" map anchor)))

(rum/defc map-base <
  {:did-mount (fn [state]
                (let [el       (rum->element state)
                      options  #js {:zoom   default-zoom
                                    :center default-center}
                      map      (js/google.maps.Map. el options)
                      topojson (-> state (:rum/args) (first))
                      geojson  (js/topojson.feature topojson
                                 (o/oget topojson "objects.wake-precincts"))]
                  (o/ocall map "data.addGeoJson" geojson)
                  (o/ocall map "data.setStyle"
                    #(precinct-style (o/ocall % "getProperty" "name")))
                  (o/ocall map "data.addListener" "click" #(click-precinct % map)))
                state)}
  [topojson]
  [:.map-base {:style {:height "98vh"}}])

(defn render
  [geojson]
  (when-let [base-div (gdom/getElement "app")]
    (rum/mount (map-base geojson) base-div)))

(defn load-json []
  (->> (http/get client/client "data/NC/nc-wake-precincts.json")
    (p/map (fn [resp]
             (reset! topojson-data
               (-> resp :body js/JSON.parse))
             (http/get client/client "data/NC/wake-results.json")))
    (p/map (fn [resp]
             (reset! results-data
               (-> resp :body js/JSON.parse js->clj))
             (render @topojson-data)))))

(defn init! []
  (load-json))

(defn reload! []
  (enable-console-print!)
  ;; Re-render the UI from the root on reload
  (render @topojson-data))
