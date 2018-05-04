(ns leiningen.new.jdbc-data-mover
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "jdbc-data-mover"))

(defn jdbc-data-mover
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (main/info "Generating fresh 'lein new' jdbc-data-mover project.")
    (->files data
             ["project.clj"                 (render "project.clj" data)]
             ["README.md"                   (render "README.md" data)]
             ["dbc/README.md"               (render "dbc/README.md" data)]
             ["dbc/derby-source.edn"        (render "dbc/derby-source.edn" data)]
             ["dbc/derby-target.edn"        (render "dbc/derby-target.edn" data)]
             ["dbc/postgres.example.edn"    (render "dbc/postgres.example.edn" data)]
             ["Drivers/README.md"           (render "Drivers/README.md" data)]
             ["src/{{sanitized}}/core.clj"  (render "src/core.clj" data)]
             ["src/{{sanitized}}/xform.clj" (render "src/xform.clj" data)]
             ["project.clj"                 (render "project.clj" data)])))

