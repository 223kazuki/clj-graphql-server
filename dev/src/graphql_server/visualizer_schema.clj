(ns graphql-server.visualizer-schema
  (:require [integrant.core :as ig]
            [figwheel.main.api :as fig]
            [clojure.java.io :as io]))

(defmethod ig/init-key :graphql-server/visualizer-schema [_ _]
  (fig/start {:id "dev"
              :options {:main 'graphql-server.visualizer
                        :output-to "resources/public/cljs-out/main.js"
                        :output-dir "resources/public/cljs-out/dev"}
              :config {:watch-dirs ["dev/src" "resources/graphql_server"]
                       :mode :serve}}))

(defmethod ig/suspend-key! :graphql-server/visualizer-schema [_ _]
  (.setLastModified (io/file "dev/src/graphql_server/visualizer.cljs")
                    (System/currentTimeMillis))
  (println "suspend"))

(defmethod ig/resume-key :graphql-server/visualizer-schema [_ _ _ _]
  (println "resume"))

(defmethod ig/halt-key! :graphql-server/visualizer-schema [_ _]
  (println "stop")
  (fig/stop "dev"))
