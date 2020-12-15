(ns goophi.core-test
  (:require [clojure.test :refer :all]
            [goophi.core :refer :all]))

(deftest item
  (testing "->Item"
    (let [item (->Item "1" "hello world" "/" "localhost" 70)]
      (is (= (:type item) "1"))
      (is (= (:display-text item) "hello world"))
      (is (= (:selector item) "/"))
      (is (= (:hostname item) "localhost"))
      (is (= (:port item) 70))
      (is (= (str item) "1hello world\t/\tlocalhost\t70\r\n"))
      (is (Item? item))))
  (testing "info"
    (let [item (info "foobar")]
      (is (= (:type item) "i"))
      (is (= (:display-text item) "foobar"))
      (is (Item? item))))
  (testing "Item?"
    (is (Item? (info "baz")))
    (is (not (Item? "foobar")))
    (is (not (Item? 23)))))

(deftest request-parsing
  (testing "empty selector"
    (let [[path query] (parse-request "")]
      (is (= path ""))
      (is (= query ""))))
  (testing "without query"
    (let [[path query] (parse-request "/foobar")]
      (is (= path "/foobar"))
      (is (= query ""))))
  (testing "with query"
    (let [[path query] (parse-request "/foo\tbar")]
      (is (= path "/foo"))
      (is (= query "bar"))))
  (testing "non-ascii path"
    (let [[path query] (parse-request "/gooφ\tphi")]
      (is (nil? path))
      (is (nil? query))))
  (testing "non-ascii query"
    (let [[path query] (parse-request "/goo\tφ")]
      (is (nil? path))
      (is (nil? query)))))
