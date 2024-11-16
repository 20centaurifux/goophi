(ns goophi.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [goophi.core :refer [->Item Item? info parse-request]]))

(deftest item
  (testing "->Item"
    (let [item (->Item "1" "hello world" "/" "localhost" 70)]
      (is (Item? item))
      (is (= "1" (:type item)))
      (is (= "hello world" (:display-text item)))
      (is (= "/" (:selector item)))
      (is (= "localhost" (:hostname item)))
      (is (= 70 (:port item)))
      (is (= "1hello world\t/\tlocalhost\t70\r\n" (str item)))))

  (testing "info"
    (let [item (info "foobar")]
      (is (Item? item))
      (is (= "i" (:type item)))
      (is (= "foobar" (:display-text item)))))

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
      (is (= "/foobar" path))
      (is (= "" query))))

  (testing "with query"
    (let [[path query] (parse-request "/foo\tbar")]
      (is (= "/foo" path))
      (is (= "bar" query))))

  (testing "non-ascii path"
    (let [[path query] (parse-request "/gooφ\tphi")]
      (is (nil? path))
      (is (nil? query))))

  (testing "non-ascii query"
    (let [[path query] (parse-request "/goo\tφ")]
      (is (nil? path))
      (is (nil? query)))))