(ns goophi.response
  (:require [clojure.string :as s]
            [goophi.core])
  (:import goophi.core.Item
           goophi.textfileentity.TextfileEntityInputStream))

(defn- print-stream
  [stream transform & {:keys [buffer-size] :or {buffer-size 8192}}]
  (loop [buffer (byte-array buffer-size)]
    (let [available (.read stream buffer)]
      (when (pos? available)
        (-> (byte-array available buffer)
            transform
            print)
        (recur buffer)))))

(defn print-text-stream
  "Reads all contents of stream and & prints it to *out*."
  [stream]
  (print-stream stream slurp))

(defn print-binary-stream
  "Reads all contents of stream & prints hexdump to *out*."
  [stream & {:keys [columns] :or {columns 16}}]
  (print-stream stream
                #(str (s/join " " (map (partial format "0x%02x") %))
                      \newline)
                :buffer-size columns))

(defprotocol MenuEntityFactory
  "Adds a final line with a period."
  (menu-entity [in]))

(extend-protocol MenuEntityFactory
  java.lang.String
  (menu-entity [text]
    (java.io.ByteArrayInputStream. (.getBytes (str (s/trimr text) "\r\n.\r\n"))))
  Item
  (menu-entity [item]
    (menu-entity (str item))))

(defprotocol TextfileEntityFactory
  "Removes control characters and appends a final line with a period."
  (text-file-entity [source]))

(extend-protocol TextfileEntityFactory
  java.io.InputStream
  (text-file-entity [in]
    (TextfileEntityInputStream. in)))

(defprotocol BinaryFactory
  "Bypasses binary data."
  (binary-entity [in]))

(extend-protocol BinaryFactory
  (Class/forName "[B")
  (binary-entity [data]
    (java.io.ByteArrayInputStream. data))
  java.io.InputStream
  (binary-entity [in]
    in))