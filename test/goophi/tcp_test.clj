(ns goophi.tcp-test
  (:require [aleph.tcp :as tcp]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [confick.core :refer [bind]]
            [goophi.response :refer [binary-entity]]
            [goophi.tcp :refer [->gopher-handler]]
            [manifold.stream :as s]))

;;; gopher client

(defn- local-client
  [port]
  @(tcp/client {:host "localhost" :port port}))

(defn- put-request!
  [c selector]
  @(s/put! c (str selector "\r\n")))

(defn- take-response!
  [c]
  (String. @(s/take! c)))

(defonce ^:private selector-gen
  (gen/fmap #(str/trimr (str "/" %)) gen/string-ascii))

;;; gopher server

(defn- start-server
  [f port]
  (tcp/start-server (->gopher-handler f) {:port port}))

;;; tests

(deftest aleph
  (bind [port [:goophi :port]]
    (letfn [(return-selector [req]
              (-> req :path .getBytes java.io.ByteArrayInputStream.))]
      (testing "roundtrip"
        (let [s (start-server return-selector port)
              property (prop/for-all [selector selector-gen]
                                     (let [c (local-client port)]
                                       (put-request! c selector)
                                       (= selector (take-response! c))))]
          (is (:result (tc/quick-check 100 property)))
          (.close s)))

      (testing "client timeout"
        (with-redefs [goophi.tcp/timeout-millis 500]
          (let [s (start-server return-selector port)
                c (local-client port)
                response (take-response! c)
                lines (str/split-lines response)]
            (is (= 2 (count lines)))
            (is (= "iConnection timeout.	fake	(NULL)	0" (lines 0)))
            (is (= "." (lines 1)))
            (.close s))))

      (testing "handler throws exception"
        (let [s (start-server (fn [_] (throw (Exception.))) port)
              c (local-client port)]
          (put-request! c "/")
          (let [response (take-response! c)
                lines (str/split-lines response)]
            (is (= 2 (count lines)))
            (is (= "iInternal Server Error.	fake	(NULL)	0" (lines 0)))
            (is (= "." (lines 1)))
            (.close s))))

      (testing "handler returns nil"
        (let [s (start-server (constantly nil) port)
              c (local-client port)]
          (put-request! c "/")
          (let [response (take-response! c)
                lines (str/split-lines response)]
            (is (= 2 (count lines)))
            (is (= "iNot Found.	fake	(NULL)	0" (lines 0)))
            (is (= "." (lines 1)))
            (.close s))))

      (testing "handler returns empty response"
        (let [s (start-server (fn [_] (binary-entity (byte-array 0))) port)
              c (local-client port)]
          (put-request! c "/")
          (is (nil? @(s/take! c)))
          (.close s))))))

(deftest aleph-with-middleware
  (bind [port [:goophi :port]]
    (testing "roundtrip"
      (let [counter (volatile! 0)]
        (letfn [(assoc-body [req]
                  (assoc req :body (-> req :path str/reverse)))

                (stream-body [req]
                  (-> req :body .getBytes java.io.ByteArrayInputStream.))

                (inc-counter [req]
                  (vswap! counter inc)
                  req)]
          (let [s (start-server (comp inc-counter stream-body assoc-body) port)
                property (prop/for-all [selector selector-gen]
                                       (let [c (local-client port)]
                                         (put-request! c selector)
                                         (= (str/reverse selector)
                                            (take-response! c))))]
            (is (:result (tc/quick-check 100 property)))
            (is (= 100 @counter))
            (.close s)))))))