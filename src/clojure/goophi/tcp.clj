(ns goophi.tcp
  (:require [clojure.string :refer [trimr]]
            [goophi.core :as core]
            [goophi.response :refer [take! menu-entity]]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce ^:private max-request 128)
(defonce ^:private transfer-chunk-size 8192)
(defonce ^:private timeout-millis 5000)

(defn- handle-request
  [app request]
  (try
    (if-let [response (app request)]
      response
      (menu-entity (core/info "Not found.")))
    (catch Exception _ (menu-entity (core/info "Internal Server Error.")))))

(defn- put-response!
  [in out]
  (when in
    (d/loop [buffer (byte-array transfer-chunk-size)]
      (let [available (take! in buffer)]
        (when (pos? available)
          (s/put! out (byte-array (take available buffer)))
          (d/recur buffer))))))

(defn- ->path
  [data]
  (let [text (String. (byte-array data))]
    (trimr text)))

(defn- split-bytes
  [data separator]
  (split-with #(not= % (byte separator)) data))

(defn- exceeds-maximum?
  [data]
  (> (count data) max-request))

(defn ->gopher-handler
  "Creates an Aleph handler."
  [app]
  (fn [s info]
    (let [request (select-keys info [:remote-addr])]
      (d/chain
       (d/loop [buffer []]
         (d/let-flow [data (s/try-take! s nil timeout-millis ::timeout)]
           (if (#{::timeout} data)
             (s/put! s (menu-entity (core/info "Connection timeout.")))
             (when data
               (let [[l r] (split-bytes data \newline)
                     buffer' (concat buffer l)]
                 (if (exceeds-maximum? buffer')
                   (s/put! s (menu-entity (core/info "Request too long.")))
                   (if (seq r)
                     (let [request' (assoc request :path (->path buffer'))]
                       (put-response! (handle-request app request') s))
                     (d/recur buffer'))))))))
       (fn [_]
         (s/close! s))))))