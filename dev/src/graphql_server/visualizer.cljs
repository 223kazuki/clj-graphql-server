(ns graphql-server.visualizer
  (:require [hodur-engine.core :as engine]
            [hodur-visualizer-schema.core :as visualizer])
  (:require-macros [graphql-server.macro :refer [read-schema]]))

(-> (read-schema "graphql_server/schema.edn")
    engine/init-schema
    visualizer/schema
    visualizer/apply-diagram!)
