(ns goophi.routing-test
  (:require [clojure.test :refer [deftest testing is]]
            [goophi.routing :refer [routes route]]))

(defn- execute-handler
  [handler path]
  (handler {:path path}))

(deftest routing
  (testing "simple route"
    (let [r (route "/foo" [] 23)]
      (is (nil? (execute-handler r "/bar")))
      (is (= 23 (execute-handler r "/foo")))))

  (testing "parameters"
    (let [r (route
             "/concat/:prefix/:suffix"
             [prefix suffix]
             (str prefix suffix))]
      (is (= "helloworld" (execute-handler r "/concat/hello/world")))))

  (testing "request map"
    (let [r (route
             "/:prefix/:suffix"
             [prefix suffix :as req]
             req)
          response (execute-handler r "/hello/world\tbaz")]
      (is (= "/hello/world" (:path response)))
      (is (= "baz" (:query response)))
      (is (= "hello" (get-in response [:params :prefix])))
      (is (= "world" (get-in response [:params :suffix])))))

  (testing "multiple routes"
    (let [rx (routes
              ("/foo" [] :foo)
              ("/bar" [] :bar)
              ("*" [] :baz))]
      (is (= :foo (execute-handler rx "/foo")))
      (is (= :bar (execute-handler rx "/bar")))
      (is (= :baz (execute-handler rx "/foobar"))))))