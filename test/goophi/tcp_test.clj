(ns goophi.tcp-test
  (:require [aleph.tcp :as tcp]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [goophi.response :as rsp]
            [goophi.tcp :refer [aleph-handler]]
            [goophi.test-utils :as utils]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

;;; gopher server

(defonce ^:private port 7070)

(defn- start-server
  [handler port]
  (tcp/start-server (aleph-handler handler) {:port port}))

;;; gopher client

(defn- local-client
  [port]
  @(tcp/client {:host "localhost" :port port}))

(defn- put-request!
  [c selector]
  @(s/put! c (str selector "\r\n")))

(defn- take!
  [source]
  @(with-open [buffer (java.io.ByteArrayOutputStream.)]
     (d/chain
      (s/consume (fn [msg]
                   (.write buffer msg))
                 source)
      (fn [_]
        (.toByteArray buffer)))))

(defn- take-str!
  [c]
  (String. (take! c)))

;;; tests

(defonce ^:private selector-gen
  (gen/fmap #(str/trimr (str "/" %)) gen/string-ascii))

(defn- return-selector
  [req]
  (-> req :path .getBytes java.io.ByteArrayInputStream.))

(deftest tcp-server
  (testing "roundtrip"
    (with-open [_ (start-server return-selector port)]
      (let [property (prop/for-all [selector selector-gen]
                                   (with-open [c (local-client port)]
                                     (put-request! c selector)
                                     (= selector (take-str! c))))]
        (is (:result (tc/quick-check 100 property))))))

  (testing "client timeout"
    (with-redefs [goophi.tcp/timeout-millis 500]
      (with-open [_ (start-server return-selector port)
                  c (local-client port)]
        (let [response (take-str! c)
              lines (str/split-lines response)]
          (is (= 2 (count lines)))
          (is (= "iConnection timeout.	fake	(NULL)	0" (lines 0)))
          (is (= "." (lines 1)))))))

  (testing "handler throws exception"
    (with-open [_ (start-server (fn [_]
                                  (throw (Exception.)))
                                port)
                c (local-client port)]
      (put-request! c "/")
      (let [response (take-str! c)
            lines (str/split-lines response)]
        (is (= 2 (count lines)))
        (is (= "iInternal server error.	fake	(NULL)	0" (lines 0)))
        (is (= "." (lines 1))))))

  (testing "handler returns nil"
    (with-open [_ (start-server (constantly nil) port)
                c (local-client port)]
      (put-request! c "/")
      (let [response (take-str! c)
            lines (str/split-lines response)]
        (is (= 2 (count lines)))
        (is (= "iNot found.	fake	(NULL)	0" (lines 0)))
        (is (= "." (lines 1))))))

  (testing "handler returns empty response"
    (with-open [_ (start-server (fn [_]
                                  (rsp/binary-entity (byte-array 0)))
                                port)
                c (local-client port)]
      (put-request! c "/")
      (is (nil? @(s/take! c))))))

(deftest binary-transfer
  (testing "roundtrip"
    (with-open [_ (start-server (fn [_]
                                  (rsp/binary-entity (utils/open-binary-file)))
                                port)
                c (local-client port)
                stream (utils/open-binary-file)]
      (put-request! c "/")
      (let [expected (utils/stream->byte-array stream)
            actual (take! c)]
        (is (= (seq expected) (seq actual)))))))

(deftest text-transfer
  (testing "roundtrip"
    (with-open [_ (start-server (fn [_]
                                  (rsp/text-file-entity (utils/open-text-file)))
                                port)
                c (local-client port)
                stream (utils/open-text-file)]
      (put-request! c "/")
      (let [contents (slurp stream)
            expected (str (str/replace contents "\n" "\r\n") ".\r\n")
            actual (take-str! c)]
        (is (= expected actual))))))