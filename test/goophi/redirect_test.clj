(ns goophi.redirect-test
  (:require [clojure.test :refer :all]
            [goophi.redirect :refer :all]))

(deftest parse-selector
  (testing "valid URI selector"
    (let [url (selector->url "URI:https://example.org:443/search?q=42")]
      (is (= (.getHost url) "example.org"))
      (is (= (.getPort url) 443))
      (is (= (.getPath url) "/search"))
      (is (= (.getQuery url) "q=42"))))
  (testing "invalid URI selector"
    (let [url (selector->url "URI:example.org:443/search?q=42")]
      (is (nil? url))))
  (testing "redirect"
    (let [html (slurp (redirect "https://localhost"))]
      (is (some?
           (re-matches #"(?is)<!DOCTYPE html>\s?<html.*</html>\s?"
                       html)))
      (is (some?
           (re-matches #"(?is).*<a href=\"https://localhost.*"
                       html))))))
