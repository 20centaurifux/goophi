(ns goophi.tcp
  (:require [manifold.stream :as s]
            [aleph.tcp :as tcp]
            [clojure.core.async :as async]
            [goophi.core :as core]
            [goophi.config :as config]
            [goophi.response :refer [take! ->text TextFactory]])
  (:import [java.io InputStream ByteArrayInputStream]
           [goophi.core Item]))

(defonce ^:private max-request 128)
(defonce ^:private transfer-chunk-size 8192)
(defonce ^:private timeout-millis 5000)

(extend Item
  TextFactory
  {:->text #(->text (str %))})

(defn- put-response!
  [in out]
  (when in
    (loop [buffer (byte-array transfer-chunk-size)]
      (let [available (take! in buffer)]
        (when (>= available 0)
          @(s/put! out (byte-array (take available buffer)))
          (recur buffer))))))

(defn- apply-handler
  [data]
  (config/bind [^:required document-path [:runtime :document-path]]
               (let [request (String. (byte-array data))]
                 (try
                   (->text (core/info "hello world"))
                   (catch Exception e (core/info (str "Internal Server Error: " (.getMessage e))))))))

(defn- request-str
  [data]
  (let [text (String. (byte-array data))]
    (clojure.string/trimr text)))

(defn- route
  [routes request]
  (->text (core/info request)))

(defn- split-bytes
  [data separator]
  (split-with (partial not= (byte separator)) data))

(defn- exceeds-maximum?
  [data]
  (> (count data) max-request))

(defn- handle-connection
  [in out routes]
  (future
    (loop [buffer nil]
      (if-let [data (async/alt!! [in (async/timeout timeout-millis)] ([v _] v))]
        (let [[l r] (split-bytes data \newline)
              buffer' (concat buffer l)]
          (if (exceeds-maximum? buffer')
            (s/put! out (->text (core/info "Request too long.")))
            (if-not (empty? r)
              (put-response! (route routes (request-str l)) out)
              (recur buffer'))))
        (s/put! out (->text (core/info "Connection timeout.")))))
    (s/close! out)))

(defn gopher-handler
  [s info]
  (let [in (async/chan)]
    (handle-connection in s [])
    (s/connect s in)))

(when-let [s' (resolve 's)]
  (.close @s'))

(def s
  (tcp/start-server
   gopher-handler
   {:port 1337}))
