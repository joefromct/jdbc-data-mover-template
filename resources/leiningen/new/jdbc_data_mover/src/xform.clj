{{! Change mustache delimiter to <% and %> }}
{{=<% %>=}}
(ns <%sanitized%>.xform
  (:require [clojure.data.json :as json]))

(defn ^String md5 [s]
  "Example of returning md5 hash of strings."
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn add-json[record]
  (assoc record :record_as_json (json/write-str record)))

(defn add-json-md5[record]
  (assoc record :json_md5 (md5 (:record_as_json record))))

(defn xform-add-json[batch-of-records]
  "Transformation that adds the entire record as a json element to the input
  record.

  Since input records are partitioned by what we want to bulk load, this
  has to be mapped across each inbound record. . "
  (map add-json batch-of-records))

(defn xform-add-json-md5[batch-of-records]
  "Map a md5 hash of the json value onto each map in the input batch."
    (map add-json-md5 batch-of-records))

(defn xform-update-columndatatype[batch-of-records]
  "Update each map in the input batch to have the datatype for column cast to
  string."
  (map #(update-in % [:columndatatype] str) batch-of-records))

<%! Reset mustache delimiter %>
<%={{ }}=%>
