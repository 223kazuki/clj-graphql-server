(defproject graphql-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [duct/core "0.7.0"]
                 [duct/module.logging "0.4.0"]
                 [duct/module.web "0.7.0"]
                 [duct/module.sql "0.5.0"]
                 [duct.module.pedestal "2.0.0"]
                 [org.postgresql/postgresql "42.2.5"]
                 [com.datomic/datomic-free "0.9.5697" :exclusions [com.google.guava/guava]]
                 ;;[com.datomic/client-api "0.8.13"]

                 [hiccup "1.0.5"]
                 [org.clojure/core.cache "0.7.2"]
                 [org.clojure/data.json "0.2.6"]
                 [buddy/buddy-hashers "1.3.0"]

                 [com.walmartlabs/lacinia "0.31.0"]
                 [com.walmartlabs/lacinia-pedestal "0.11.0"]

                 [hodur/engine "0.1.5"]
                 [hodur/lacinia-schema "0.1.1"]
                 [hodur/datomic-schema "0.1.0"]
                 [hodur/spec-schema "0.1.0"]]
  :plugins [[duct/lein-duct "0.11.0-beta4"]]
  :main ^:skip-aot graphql-server.main
  :resource-paths ["resources" "target/resources"]
  :clean-targets ^{:protect false} ["target" "resources/public/cljs-out"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.1"]
                                   [eftest "0.5.4"]
                                   [kerodon "0.9.0"]
                                   [alembic "0.3.2"]
                                   [hodur/visualizer-schema "0.1.1"]]}})
