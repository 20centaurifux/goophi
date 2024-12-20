(ns goophi.core
  (:require [clojure.string :as str]))

(defn- sanitize-str
  [text]
  (if (str/blank? text) "" text))

(defn- match-request
  [request]
  (re-matches #"^([\u0020-\u007e]*)\t?([\u0020-\u007e]+)?$" request))

(defn parse-request
  "Returns path and query of a gopher request. Only printable ASCII characters
  are accepted."
  [request]
  (when-let [match (match-request request)]
    (map sanitize-str (rest match))))

(defn- item->seq
  [item]
  (let [[type display-text & fields] (vals item)]
    (cons (str type display-text) fields)))

(defn- item->str
  [item]
  (str/join "\t" (item->seq item)))

(defrecord Item [type display-text selector hostname port]
  Object
  (toString [item]
    (str (item->str item) "\r\n")))

(defn Item?
  "Tests if x is an Item."
  [x]
  (instance? Item x))

(defn ->Item
  "Constructs a gopher item. Items can be serialized with str."
  ([type display-text selector hostname port]
   (Item. type display-text selector hostname port))
  ([type display-text selector hostname]
   (->Item type display-text selector hostname 70)))

(defn info
  "Constructs an information item."
  [text]
  (->Item "i" text "fake" "(NULL)" 0))