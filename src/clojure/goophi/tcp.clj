(ns goophi.tcp
  (:require [clojure.string :as str]
            [goophi.core :as goo]
            [goophi.response :as response :as rsp]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce ^:private max-request 128)
(defonce ^:private timeout-millis 5000)

(defn wrap-response
  "Returns a function that takes a request map as argument. Evaluates handler
   and adds the result to the request map with the key :response."
  [handler]
  (fn [req]
    (assoc req :response (handler req))))

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
    (let [{:keys [response]} (handler req)]
      (if response
        response
        (rsp/menu-entity (goo/info "Not Found."))))
    (catch Exception _ (rsp/menu-entity (goo/info "Internal Server Error.")))))

(defn aleph-handler
  "Creates an Aleph handler."
  [handler]
  (fn [s info]
    (let [request (select-keys info [:remote-addr])]
      (d/chain
       (d/loop [buffer []]
         (d/let-flow [data (s/try-take! s nil timeout-millis ::timeout)]
           (if (#{::timeout} data)
             (s/put! s (rsp/menu-entity (goo/info "Connection timeout.")))
             (when data
               (let [[l r] (split-bytes data \newline)
                     buffer' (concat buffer l)]
                 (if (exceeds-maximum? buffer')
                   (s/put! s (rsp/menu-entity (goo/info "Request too long.")))
                   (if (seq r)
                     (with-open [response (execute-handler handler
                                                           (assoc request
                                                                  :path (->path buffer')))]
                       (s/put! s response))
                     (d/recur buffer'))))))))
       (fn [_]
         (s/close! s))))))