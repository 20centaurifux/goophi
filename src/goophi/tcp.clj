(ns goophi.tcp
  (:require [manifold.stream :as s]
            [aleph.tcp :as tcp]
            [clojure.core.async :as async]
            [goophi.core :as core]
            [goophi.fs :as fs])
  (:import [java.io InputStream ByteArrayInputStream]))

(def ^:private base-path "/home/sf/tmp/gopher")

(defn- ->stream
  [val]
  (cond
    (instance? InputStream val) val
    (instance? String val) (->stream (.getBytes val))
    (bytes? val) (ByteArrayInputStream. val)
    :else nil))

(defn- process-request
  [data]
  (let [request (String. (byte-array data))]
    (try
      (if-let [[selector query] (core/parse-request request)]
        (let [response (fs/get-contents base-path selector)]
          (->stream response))
        "Bad Request.")
      (catch Exception e (str "Internal Server Error: " (.getMessage e))))))

(defn- split-bytes
  [data separator]
  (split-with (partial not= (byte separator)) data))

(defn- exceeds-maximum?
  [data]
  (> (count data) 128))

(defn- handle-request
  [in out]
  (async/go
    (loop [buffer nil]
      (when-let [data (async/alt! [in (async/timeout 5000)] ([v _] v))]
        (let [[l r] (split-bytes data \newline)
              buffer' (concat buffer l)]
          (if (exceeds-maximum? buffer')
            (s/put! out "Request too long.\r\n")
            (if-not (empty? r)
              (->> (drop-last l)
                   process-request
                   (s/put! out))
              (recur buffer'))))))
    (.close out)))

(defn gopher-handler
  [s info]
  (let [in (async/chan)]
    (handle-request in s)
    (s/connect s in)))

(when-let [s' (resolve 's)]
  (.close @s'))

(def s
  (tcp/start-server
   gopher-handler
   {:port 1337}))
