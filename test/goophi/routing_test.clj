(ns goophi.routing-test
  (:require [clojure.test :refer :all]
            [goophi.routing :refer :all]))

(deftest routing
  (testing "simple route"
    (let [r (->route "/foo"
                     []
                     23)]
      (is (nil? (r "/bar")))
      (is (= 23 (r "/foo")))))
  (testing "parameters"
    (let [r (->route "/concat/:prefix/:suffix"
                     [prefix suffix]
                     (str prefix suffix))]
      (is (= "helloworld" (r "/concat/hello/world")))))
  (testing "request map"
    (let [r (->route "/:prefix/:suffix"
                     [prefix suffix :as req]
                     req)
          response (r "/hello/world\tbaz")]
      (is (= "/hello/world"
             (:path response)))
      (is (= "baz"
             (:query response)))
      (is (= "hello"
             (get-in response [:params :prefix])))
      (is (= "world"
             (get-in response [:params :suffix])))))
  (testing "multiple routes"
    (let [rx (->routes ("/foo" [] :foo)
                       ("/bar" [] :bar)
                       ("*" [] :baz))]
      (is (= (rx "/foo") :foo))
      (is (= (rx "/bar") :bar))
      (is (= (rx "/foobar") :baz)))))
