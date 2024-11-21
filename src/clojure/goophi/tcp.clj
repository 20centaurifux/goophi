(ns goophi.tcp
  (:require [clojure.string :as str]
            [goophi.core :as goo]
            [goophi.response :as response :as rsp]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce ^:private max-request 128)
(defonce ^:private timeout-millis 5000)

(defn- ->path
  [data]
  (let [text (String. (byte-array data))]
    (str/trimr text)))

(defn- split-bytes
  [data separator]
  (split-with #(not= % (byte separator)) data))

(defn- exceeds-maximum?
  [data]
  (> (count data) max-request))

(defn- execute-handler
  [handler req]
  (try
    (let [response (handler req)]
      (if response
        response
        (rsp/menu-entity (goo/info "Not Found."))))
    (catch Exception _ (rsp/menu-entity (goo/info "Internal Server Error.")))))

(defn- put-stream!
  [s stream]
  (d/chain
   (s/put! s stream)
   (fn [_]
       (s/close! s)
       (.close stream))))
           
(defn aleph-handler
  "Creates an Aleph handler."
  [handler]
  (fn [s info]
    (let [request (select-keys info [:remote-addr])]
      (d/loop [buffer []]
        (d/let-flow [data (s/try-take! s nil timeout-millis ::timeout)]
          (if (#{::timeout} data)
            (put-stream! s (rsp/menu-entity (goo/info "Connection timeout.")))
            (when data
              (let [[l r] (split-bytes data \newline)
                    buffer' (concat buffer l)]
                (if (exceeds-maximum? buffer')
                  (put-stream! s (rsp/menu-entity (goo/info "Request too long.")))
                  (if (seq r)
                    (put-stream! s (execute-handler handler
                                                    (assoc request
                                                           :path (->path buffer'))))
                    (d/recur buffer')))))))))))