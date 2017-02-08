(set-env!
  :resource-paths #{"resources"}
  :source-paths #{"src" "data"}
  :dependencies '[[semantic-csv "0.1.0"]
                  [org.clojure/data.json "0.2.6"]

                  [org.clojure/clojurescript "1.9.456"]
                  [binaryage/oops  "0.5.2"]
                  [adzerk/env      "0.3.0"]

                  [funcool/promesa "1.6.0"]
                  [funcool/httpurr "0.6.2"]

                  [cljsjs/react "15.3.1-0"]
                  [cljsjs/react-dom "15.3.1-0"]
                  [sablono "0.7.5"]
                  [rum "0.10.7"]
                  [cljsjs/google-maps "3.18-1"]
                  [cljsjs/topojson "1.6.18-0"]

                  [thi.ng/color "1.2.0"]
                  [adzerk/boot-cljs   "1.7.228-1" :scope "test"]
                  [adzerk/boot-reload "0.4.12" :scope "test"]
                  [pandeiro/boot-http "0.7.3" :scope "test"]
                  [afrey/boot-asset-fingerprint "1.2.0" :scope "test"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[afrey.boot-asset-fingerprint :refer [asset-fingerprint]])

(defn set-development-env! []
  (task-options!
    reload {:on-jsload 'elections.core/reload!}
    cljs {:optimizations    :none
          :source-map       true}
    asset-fingerprint {:skip true}))

(deftask build []
  (comp
    (speak)
    (cljs)
    (asset-fingerprint)))

(deftask dev
  "Run application in development mode"
  []
  (set-development-env!)
  (comp
    (watch)
    (reload)
    (build)
    (serve :port 3005)))

(defn set-production-env! []
  (task-options!
    cljs {:optimizations :advanced}))

(deftask prod
  []
  (set-production-env!)
  (comp
    (build)
    (sift :invert true :include #{#"^main.out" #"^main.cljs.edn" #".txt$"})
    (target)))

(deftask local-prod []
  (set-production-env!)
  (comp
    (watch)
    (build)
    (serve :port 3005)))
