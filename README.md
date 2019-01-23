# graphql-server

Exmaple clojure graphql server. It uses following technology stacks.

* pedestal ... Web server.
* lacinia ... Clojure GraphQL implementation.
* hodur ... Domain modeling tool.
* OAuth2 ... Token based authentication for web API.
* datomic ... A transactional database.

## Developing

### Setup

When you first clone this repository, run:

```sh
lein duct setup
```

This will create files for local configuration, and prep your system
for the project.

#### Start datomic-free

Download datomic-free-0.9.5697.zip from [here](https://my.datomic.com/downloads/free).

```
$ unzip datomic-free-0.9.5697.zip
$ cd datomic-free-0.9.5697
$ bin/transactor config/samples/free-transactor-template.properties
```

#### Setup schema visualization

In dev profile, schma difined in edn file will be visualized by [hodur-visualizer-schema](https://github.com/luchiniatwork/hodur-visualizer-schema).
Then you need [GoJS](https://gojs.net/latest/index.html)

```sh
$ mkdir -p resources/public/scripts
$ curl https://gojs.net/latest/release/go.js -o resources/public/scripts/go.js
```

### Environment

To begin developing, start with a REPL.

```sh
lein do clean, repl
```

Then load the development environment.

```clojure
user=> (dev)
:loaded
```

Run `go` to prep and initiate the system.

```clojure
dev=> (go)
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kazukitsutsumi/.m2/repository/org/slf4j/slf4j-nop/1.7.25/slf4j-nop-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kazukitsutsumi/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.helpers.NOPLoggerFactory]
WARNING: requiring-resolve already refers to: #'clojure.core/requiring-resolve in namespace: datomic.common, being replaced by: #'datomic.common/requiring-resolve
Migrated

Creating your [DEV] server...
[Figwheel:WARNING] The watch directory "resources/graphql_server" is not the classpath! A watch directory is must on the classpath and point to the root directory of your namespace source tree. A general all encompassing watch directory will not work.
[Figwheel:WARNING] Attempting to dynamically add "resources/graphql_server" to classpath!
[Figwheel:WARNING] Source directory "resources/graphql_server" is not on the classpath
[Figwheel:WARNING] Please fix this by adding "resources/graphql_server" to your classpath
 I.E.
 For Clojure CLI Tools in your deps.edn file:
    ensure "resources/graphql_server" is in your :paths key

 For Leiningen in your project.clj:
   add it to the :source-paths key

[Figwheel] Compiling build dev to "resources/public/cljs-out/main.js"
[Figwheel] Successfully compiled build dev to "resources/public/cljs-out/main.js" in 17.441 seconds.
[Figwheel] Watching paths: ("dev/src" "resources/graphql_server") to compile build - dev
[Figwheel] Starting Server at http://localhost:9500
Opening URL http://localhost:9500
:initiated
```

It create two servers in dev profile by default.

* http://localhost:9500 ... Schema visualization by [hodur-visualizer-schema](https://github.com/luchiniatwork/hodur-visualizer-schema).
* http://localhost:8080 ... GraphQL endpoint. It also hosts graphiql authentication routes.

When you make changes to your source files, use `reset` to reload any
modified files and reset the server.

```clojure
dev=> (reset)
:reloading (...)
Migrated

Creating your [DEV] server...
:resumed
```

After reset application, hodur-visualizer-schema will be reloaded if you change schema.edn.
But you have to reload graphiql console by yourself because it has access token in its html.

### Testing

Testing is fastest through the REPL, as you avoid environment startup time.

```clojure
dev=> (test)
...
```

But you can also run tests through Leiningen.

```sh
lein test
```
