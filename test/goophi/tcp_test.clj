(ns goophi.tcp-test
  (:require [aleph.tcp :as tcp]
            [clojure.test :refer :all]
            [confick.core :refer [bind]]
            [goophi.response :refer :all]
            [goophi.routing :refer :all]
            [goophi.tcp :refer :all]
            [manifold.stream :as s]))

(defn- ->echo-server
  []
  (->route "*" [:as req] (-> req
                             :path
                             .getBytes
                             java.io.ByteArrayInputStream.)))

(deftest aleph
  (testing "tcp echo server"
    (bind [port [:goophi :port]]
      (let [s (tcp/start-server
               (->gopher-handler (->echo-server))
               {:port port})]
        (doseq [selector ["/foo" "/bar" "/baz"]]
          (let [c @(tcp/client {:host "localhost" :port port})]
            @(s/put! c (str selector "\r\n"))
            (let [response (String. @(s/take! c))]
              (is (= response selector)))))
        (.close s)))))

(deftest aleph-with-middleware
  (testing "tcp echo server with middleware"
    (bind [port [:goophi :port]]
      (let [n (atom 0)
            s (tcp/start-server
               (->gopher-handler (comp
                                  (->echo-server)
                                  (fn [request]
                                    (swap! n inc)
                                    request)))
               {:port port})]
        (doseq [_ (range 5)]
          (let [c @(tcp/client {:host "localhost" :port port})]
            @(s/put! c "/\r\n")
            @(s/take! c)))
        (.close s)
        (is (= 5 @n))))))