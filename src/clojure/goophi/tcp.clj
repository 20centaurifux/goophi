(ns goophi.tcp
  (:require [manifold.stream :as s]
            [aleph.tcp :as tcp]
            [clojure.core.async :as async]
            [goophi.core :as core]
            [goophi.config :as config]
            [goophi.response :refer [take! menu-entity]]
            [goophi.routing :refer [defroutes]]
            [goophi.fs :refer [get-contents]])
  (:import [java.io InputStream ByteArrayInputStream]
           [goophi.core Item]))

(defonce ^:private max-request 128)
(defonce ^:private transfer-chunk-size 8192)
(defonce ^:private timeout-millis 5000)

(defn- put-response!
  [in out]
  (when in
    (loop [buffer (byte-array transfer-chunk-size)]
      (let [available (take! in buffer)]
        (when (>= available 0)
          @(s/put! out (byte-array (take available buffer)))
          (recur buffer))))))

(defn- route-request
  [routes request]
  (try
    (if-let [response (some #(% request) routes)]
      response
      (menu-entity (core/info "Not found.")))
    (catch Exception e (menu-entity (core/info "Internal Server Error.")))))

(defn- request-str
  [data]
  (let [text (String. (byte-array data))]
    (clojure.string/trimr text)))

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
            (s/put! out (menu-entity (core/info "Request too long.")))
            (if-not (empty? r)
              (put-response! (route-request routes (request-str l)) out)
              (recur buffer'))))
        (s/put! out (menu-entity (core/info "Connection timeout.")))))
    (s/close! out)))

(defn ->gopher-handler
  "Creates an Aleph handler."
  [routes]
  (fn [s info]
    (let [in (async/chan)]
      (handle-connection in s routes)
      (s/connect s in))))
