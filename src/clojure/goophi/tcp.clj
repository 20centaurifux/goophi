(ns goophi.tcp
  (:require [clojure.core.async :as async]
            [goophi.core :as core]
            [goophi.response :refer [take! menu-entity]]
            [manifold.stream :as s]))

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
  [app request]
  (try
    (if-let [response (app request)]
      response
      (menu-entity (core/info "Not found.")))
    (catch Exception _ (menu-entity (core/info "Internal Server Error.")))))

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
  [in out app request]
  (future
    (loop [buffer nil]
      (if-let [data (async/alt!!
                      [in (async/timeout timeout-millis)]
                      ([v _] v))]
        (let [[l r] (split-bytes data \newline)
              buffer' (concat buffer l)]
          (if (exceeds-maximum? buffer')
            (s/put! out (menu-entity (core/info "Request too long.")))
            (if-not (empty? r)
              (put-response! (route-request
                              app
                              (assoc request :path (request-str l)))
                             out)
              (recur buffer'))))
        (s/put! out (menu-entity (core/info "Connection timeout.")))))
    (s/close! out)))

(defn ->gopher-handler
  "Creates an Aleph handler."
  [app]
  (fn [out info]
    (let [in (async/chan)]
      (handle-connection
       in
       out
       app
       (select-keys info [:remote-addr]))
      (s/connect out in))))