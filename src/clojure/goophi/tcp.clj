(ns goophi.tcp
  (:require [clojure.string :as str]
            [goophi.core :refer [info]]
            [goophi.response :refer [menu-entity]]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce ^:private max-request 128)
(defonce ^:private timeout-millis 5000)
(defonce ^:private buffer-size 8192)

(defn- try-close!
  [s]
  (try
    (s/close! s)
    (catch Exception _)))

(defn- put-response!
  [s stream]
  (let [buffer (byte-array buffer-size)]
    (-> (d/loop []
          (let [available (.read stream buffer)]
            (when (pos? available)
              (s/put! s (byte-array available buffer))
              (d/recur))))
        (d/finally #(try-close! stream)))))

(defn- execute-handler
  [handler req]
  (try
    (let [response (handler req)]
      (if response
        response
        (menu-entity (info "Not found."))))
    (catch Exception _ (menu-entity (info "Internal server error.")))))

(defn aleph-handler
  "Creates an Aleph handler."
  [handler]
  (fn [s inf]
    (let [request (select-keys inf [:remote-addr])
          buffer (java.io.ByteArrayOutputStream.)]
      (-> (d/loop []
            (d/let-flow [data (s/try-take! s nil timeout-millis ::timeout)]
              (if (#{::timeout} data)
                (put-response! s (menu-entity (info "Connection timeout.")))
                (when data
                  (.write buffer data)
                  (cond
                    (> (.size buffer) max-request)
                    (put-response! s (menu-entity (info "Request too long.")))

                    (= (aget data (dec (alength data))) 10)
                    (->> (.toString buffer "US-ASCII")
                         str/trimr
                         (assoc request :path)
                         (execute-handler handler)
                         (put-response! s))

                    :else
                    (d/recur))))))
          (d/finally #(try-close! s))))))