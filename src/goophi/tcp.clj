(ns goophi.tcp
  (:require [manifold.stream :as s]
            [aleph.tcp :as tcp]
            [clojure.core.async :as async]
            [goophi.core :as core]
            [goophi.fs :as fs]
            [goophi.redirect :as r]
            [goophi.config :as config])
  (:import [java.io InputStream ByteArrayInputStream]))

(defn- ^InputStream response->stream
  [val]
  (cond
    (instance? InputStream val) val
    (instance? String val) (response->stream (.getBytes ^String val))
    (core/Item? val) (response->stream (str val))
    (bytes? val) (ByteArrayInputStream. val)))

(defn- put-response!
  [out response]
  (when response
    (with-open [stream (response->stream response)]
      (loop [buffer (byte-array 8192)]
        (let [available (.read stream buffer)]
          (when (>= available 0)
            @(s/put! out (byte-array (take available buffer)))
            (recur buffer)))))))

(defn- apply-handler
  [data]
  (config/bind [^:required document-path [:runtime :document-path]]
    (let [request (String. (byte-array data))]
      (try
        (if-let [[selector query] (core/parse-request request)]
          (if-let [match (re-matches #"^URL:(.*)" selector)]
            (r/redirect (second match))
            (fs/get-contents document-path selector))
          (core/error "Bad Request."))
        (catch Exception e (core/error (str "Internal Server Error: " (.getMessage e))))))))

(defn- split-bytes
  [data separator]
  (split-with (partial not= (byte separator)) data))

(defn- exceeds-maximum?
  [data]
  (> (count data) 128))

(defn- handle-request
  [in out]
  (future
    (loop [buffer nil]
      (when-let [data (async/alt!! [in (async/timeout 5000)] ([v _] v))]
        (let [[l r] (split-bytes data \newline)
              buffer' (concat buffer l)]
          (if (exceeds-maximum? buffer')
            (s/put! out "Request too long.\r\n")
            (if-not (empty? r)
              (->> (drop-last l)
                   apply-handler
                   (put-response! out))
              (recur buffer'))))))
    (s/close! out)))

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
