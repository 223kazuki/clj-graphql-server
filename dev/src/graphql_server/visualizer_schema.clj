(ns graphql-server.visualizer-schema
  (:require [integrant.core :as ig]
            [figwheel.main.api :as fig]))

(defmethod ig/init-key :graphql-server/visualizer-schema [_ options]
  (fig/start {:id "dev"
              :options {:main 'graphql-server.visualizer
                        :output-to "resources/public/cljs-out/main.js"
                        :output-dir "resources/public/cljs-out/dev"}
              :config {:watch-dirs ["dev/src" "resources/graphql_server"]
                       :mode :serve}}))

(defmethod ig/halt-key! :graphql-server/visualizer-schema [_ _]
  (fig/stop "dev"))
