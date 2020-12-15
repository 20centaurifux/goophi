(ns goophi.response
  (:require [goophi.core])
  (:import [goophi.core Item]
           [goophi.textfileentity TextfileEntityInputStream]))

(defprotocol Response
  "Response data source."
  (take! [in out]))

(defn Response?
  "Returns true if x is a Response."
  [x]
  (satisfies? Response x))

(defn- dump
  [response transform & {:keys [buffer-size] :or {buffer-size 8192}}]
  (loop [buffer (byte-array buffer-size)]
    (let [available (take! response buffer)]
      (when (>= available 0)
        (-> (take available buffer)
            byte-array
            transform
            print)
        (recur buffer)))))

(defn dumps
  "Reads response as string & prints it to *out*."
  [response]
  (dump response slurp))

(defn dumpx
  "Reads response & prints hexdump to *out*."
  [response & {:keys [columns] :or {columns 16}}]
  (dump response
        #(str
          (clojure.string/join " " (map (partial format "0x%02x") %))
          \newline)
        :buffer-size columns))

(extend java.io.InputStream
  Response
  {:take! #(.read % %2)})

(defprotocol MenuEntityFactory
  "Appends a period to the response."
  (menu-entity [in]))

(extend-protocol MenuEntityFactory
  java.lang.String
  (menu-entity [text]
    (java.io.ByteArrayInputStream. (.getBytes (str
                                               (clojure.string/trimr text)
                                               "\r\n.\r\n"))))
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
