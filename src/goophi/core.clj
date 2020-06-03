(ns goophi.core
  (:require [clojure.string :as s]))

(defn- sanitize-str
  [text]
  (if (s/blank? text) "" text))

(defn- request-matches
  [request]
  (re-matches #"^([\u0020-\u007e]*)\t?([\u0020-\u007e]+)?$" request))

(defn parse-request
  "Returns path and query of a gopher request. Only printable ASCII characters
  are accepted."
  [request]
  (when-let [match (request-matches request)]
    (map sanitize-str (rest match))))

(defn- item->seq
  [item]
  (let [[type display-text & fields] (vals item)]
    (cons (str type display-text) fields)))

(defn- item->str
  [item]
  (s/join "\t" (item->seq item)))

(defrecord Item
           [type display-text selector hostname port]
  Object
  (toString [item]
    (str (item->str item) "\r\n")))

(defn ->Item
  "Constructs a gopher item. Items can be serialized with str."
  ([type display-text selector hostname port]
   (Item. type display-text selector hostname port))
  ([type display-text selector hostname]
   (Item. type display-text selector hostname 70)))
