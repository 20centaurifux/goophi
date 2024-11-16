(ns goophi.redirect-test
  (:require [clojure.test :refer [deftest testing is]]
            [goophi.redirect :refer [redirect selector->url]]))

(deftest parse-selector
  (testing "valid URI selector"
    (let [url (selector->url "URI:https://example.org:443/search?q=42")]
      (is (= "example.org" (.getHost url)))
      (is (= 443 (.getPort url)))
      (is (= "/search" (.getPath url)))
      (is (= "q=42" (.getQuery url)))))

  (testing "invalid URI selector"
    (let [url (selector->url "URI:example.org:443/search?q=42")]
      (is (nil? url))))

  (testing "redirect"
    (let [html (slurp (redirect "https://localhost"))]
      (is (some? (re-matches #"(?is)<!DOCTYPE html>\s?<html.*</html>\s?" html)))
      (is (some? (re-matches #"(?is).*<a href=\"https://localhost.*" html))))))