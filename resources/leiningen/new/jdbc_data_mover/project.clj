{{! Change mustache delimiter to <% and %> }}
{{=<% %>=}}
(defproject <%sanitized%> "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.clojure/core.async "0.4.474"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [org.apache.derby/derby "10.14.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 ;; if your source/target db's are in maven/clojars:
                 ;;[org.postgresql/postgresql "42.2.2"]
                 ]
  :main ^:skip-aot <%sanitized%>.core
  :target-path "target/%s"
  ;;:jvm-opts ["-Xmx100G" "-Xms100G" "-server"]
  :profiles {:uberjar {:aot :all},
             :dev {:dependencies []
                   :plugins []
                   :source-paths ["env/dev" "src"]}}
  :resource-paths [
                   ;; 3rd party JDBC drivers not availible in maven to be added
                   ;; them to our classpath
                   ;; Netezza:
                   ;;"Drivers/nzjdbc.jar"
                   ;; DB2 z/os:
                   ;;"Drivers/db2jcc.jar"
                   ;;"Drivers/db2jcc_license_cu.jar"
                   ;;"Drivers/db2jcc_license_cisuz.jar"
                   ;; etc.
                   ])

<%! Reset mustache delimiter %>
<%={{ }}=%>
