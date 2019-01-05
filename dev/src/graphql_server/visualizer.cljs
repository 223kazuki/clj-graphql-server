(ns graphql-server.visualizer
  (:require [hodur-engine.core :as engine]
            [hodur-visualizer-schema.core :as visualizer]))

(def meta-db
  (engine/init-schema
   '[Person
     [^String first-name
      ^String last-name
      ^Gender gender]

     ^:enum
     Gender
     [MALE FEMALE IRRELEVANT]]))

(-> meta-db
    visualizer/schema
    visualizer/apply-diagram!)
