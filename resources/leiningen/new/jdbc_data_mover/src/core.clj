{{! Change mustache delimiter to <% and %> }}
{{=<% %>=}}
(ns <%sanitized%>.core
  (:gen-class)
  (:require [<%sanitized%>.xform :refer [xform-add-json
                                   xform-add-json-md5
                                   xform-update-columndatatype]]
            [clojure.java.jdbc :as j]
            [clojure.core.async :refer [>!! <!! chan thread] :as a]
            [jdbc.pool.c3p0 :as pool]
            [clojure.pprint :refer [pprint]]))

;; source connection options
(def source-db-spec        (-> "./dbc/derby-source.edn"  slurp read-string :db-spec))
(def target-db-spec (-> "./dbc/derby-target.edn"  slurp read-string :db-spec))
;; this is how we could do a connection pool, probably pointless in derby.
(def target-db-pool (pool/make-datasource-spec target-db-spec))

(def sql " select
                t1.tablename   ,
                t2.columnname  ,
                t2.columnnumber,
                t2.columndatatype
            from
                sys.systables as t1 inner join
                sys.syscolumns as t2 on t1.tableid = t2.REFERENCEID
            order by t1.tablename, t2.columnnumber ")

;; we'll need a target table to load
(j/execute! target-db-pool "create table my_output (
                                      tablename varchar(100),
                                      columnname varchar(100),
                                      columnnumber int,
                                      columndatatype varchar(100),
                                      record_as_json long varchar,
                                      json_md5           char(32)
                                    ) " )

(defn insert-multi-maps! [db-pool table-name-fq records]
  "same idea as j/insert-multi!, but has this first let binding to pick of the
  cols from the keys of the input records. This is really so we can work with
  records in async channels and still get the \"bulk-load\" benefits of
  insert-multi!."
  (let [cols            (map name (keys (first records)))
        values          (map #(into []  (vals %)) records)]
    (j/insert-multi! db-pool
                     table-name-fq
                     cols
                     values
                     {:transaction? true})
    1 ; batches inserted, to be reduced
    ))


(defn -main[]
  ;; this would basically be the entry point of real programs.
  (let [
        table-name "my_output"
        parallelism 1 ;; If this is turned up, you still have one reader on the
        ;; source but multiple cores working on your transducer and
        ;; transformations.  Same with output loads.
        commit-interval 10 ;; How big of the abtches to prepare-insert?
        db-ch                 (chan 5 (partition-all  commit-interval))
        return-ch             (chan 5 ) ;; pipeline needs something to output
        batches-inserted-ch   (a/reduce + 0 return-ch)
        input-reader-count    (atom 0)
        my-insert-multi-maps! (partial insert-multi-maps! target-db-pool table-name)
        row-input-fn          (fn  [row]
                                (do (>!! db-ch row) ;; this is the important bit
                                    ;; everything below is just for printing to stdout/logging
                                    (swap! input-reader-count inc)
                                    (when (zero? (mod @input-reader-count commit-interval))
                                      (println "Fetched " @input-reader-count " rows.")))
                                1 ; something to reduce
                                )]
    ;; kick off the reader
    (thread (transduce
             (map row-input-fn)
             +
             (j/reducible-query source-db-spec sql) )
            (a/close! db-ch))
    ;; pipeline the work
    (a/pipeline parallelism
                return-ch
                (map (comp my-insert-multi-maps!
                        xform-add-json-md5
                        xform-add-json
                        xform-update-columndatatype))
                db-ch)
    ;; remove back pressure from output
    (println (str "Wrote "
                  (<!! batches-inserted-ch)
                  " total partitions loaded with multi-insert!.")))
  (pprint "Here's three rows of output: ")
  (pprint (take 3 (j/query target-db-pool "select * from my_output fetch first 2 rows only"))))

;; what this looks like when run:
;;
;; Fetched  10  rows.
;; Fetched  20  rows.
;; Fetched  30  rows.
;; Fetched  40  rows.
;; Fetched  50  rows.
;; Fetched  60  rows.
;; Fetched  70  rows.
;; Fetched  80  rows.
;; Fetched  90  rows.
;; Fetched  100  rows.
;; Fetched  110  rows.
;; Fetched  120  rows.
;; Fetched  130  rows.
;; Fetched  140  rows.
;; Fetched  150  rows.
;; Wrote 16 total partitions.
;; <%sanitized%>.core>
;; see what we have on the target?
;(j/query target-db-pool "select * from my_output fetch first 2 rows only")
;;=>
;;({:tablename "SYSALIASES",
;;  :columnname "ALIAS",
;;  :columnnumber 2,
;;  :columndatatype "VARCHAR(128) NOT NULL",
;;  :record_as_json
;;  "{\"tablename\":\"SYSALIASES\",\"columnname\":\"ALIAS\",\"columnnumber\":2,\"columndatatype\":\"VARCHAR(128) NOT NULL\"}",
;;  :json_md5 "aa30a9037f83a5ce7f5a2d7b93aa7f1b"}
;; {:tablename "SYSALIASES",
;;  :columnname "SCHEMAID",
;;  :columnnumber 3,
;;  :columndatatype "CHAR(36)",
;;  :record_as_json
;;  "{\"tablename\":\"SYSALIASES\",\"columnname\":\"SCHEMAID\",\"columnnumber\":3,\"columndatatype\":\"CHAR(36)\"}",
;;  :json_md5 "589fb972e5ac2f3f93e7f3e1ba779b7b"}
;; {:tablename "SYSALIASES",
;;  :columnname "JAVACLASSNAME",
;;  :columnnumber 4,
;;  :columndatatype "LONG VARCHAR NOT NULL",
;;  :record_as_json
;;  "{\"tablename\":\"SYSALIASES\",\"columnname\":\"JAVACLASSNAME\",\"columnnumber\":4,\"columndatatype\":\"LONG VARCHAR NOT NULL\"}",
;;  :json_md5 "2b7e68015f77b725f5050977d3022f79"})

<%! Reset mustache delimiter %>
<%={{ }}=%>
