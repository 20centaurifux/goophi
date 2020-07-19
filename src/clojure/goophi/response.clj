(ns goophi.response
  (:require [clojure.java.io :as io])
  (:import [goophi.core Item]
           [goophi.textfileentity TextfileEntityInputStream]))

(defprotocol Response
  "Response data source."
  (take! [in out]))

(extend java.io.InputStream
  Response
  {:take! #(.read % %2)})

(defprotocol MenuEntityFactory
  "Appends a period to the response."
  (menu-entity [in]))

(extend-protocol MenuEntityFactory
  java.lang.String
  (menu-entity [text]
    (java.io.ByteArrayInputStream. (.getBytes (str text ".\r\n"))))
  Item
  (menu-entity [item]
    (menu-entity (str item))))

(defprotocol TextfileEntityFactory
  "Removes control characters and appends a period."
  (text-file-entity [source]))

(extend-protocol TextfileEntityFactory
  java.io.InputStream
  (text-file-entity [in]
    (TextfileEntityInputStream. in)))

(defprotocol BinaryFactory
  "Bypasses binary data."
  (binary-entity [source]))

(extend-protocol BinaryFactory
  (Class/forName "[B")
  (binary-entity [data]
    (java.io.ByteArrayInputStream. data))
  java.io.InputStream
  (binary-entity [in]
    in))
