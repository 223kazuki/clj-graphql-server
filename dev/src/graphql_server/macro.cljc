(ns graphql-server.macro
  (:require #?(:clj [clojure.java.io :as io])))

(defmacro read-schema [path]
  #?(:clj (->> path
               io/resource
               slurp
               (str "'")
               read-string)))
