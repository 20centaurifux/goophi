(ns goophi.fs-test
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.test :refer [deftest testing is]]
            [goophi.fs :refer [get-contents]]
            [goophi.response :as rsp]
            [goophi.test-utils]))

(defonce ^:private base-dir "./example-pub")
(defonce ^:private hostname "example.org")
(defonce ^:private port 7070)

(defn- roundtrip
  [selector]
  (-> (get-contents base-dir selector :hostname hostname :port port)
      rsp/print-text-stream
      with-out-str))

(deftest directory-traversal
  (testing "directory traversal"
    (is (nil? (get-contents base-dir "../")))
    (is (some? (get-contents base-dir "docs/../")))
    (is (nil? (get-contents base-dir "docs/../../")))))

(deftest menu
  (testing "gophermap"
    (doseq [selector ["" "gophermap"]]
      (let [lines (s/split-lines (roundtrip selector))]
        (is (= 4 (count lines)))
        (is (= "iWelcome!\tfake\t(NULL)\t0" (lines 0)))
        (is (= "i\tfake\t(NULL)\t0" (lines 1)))
        (is (= (format "1docs\tdocs\t%s\t%d" hostname port)
               (lines 2)))
        (is (= "." (lines 3))))))

  (testing "listing"
    (let [lines (s/split-lines (roundtrip "docs"))]
      (is (= 3 (count lines)))
      (is (= (format "0hello.txt\t/docs/hello.txt\t%s\t%d" hostname port)
             (lines 0)))
      (is (= (format "iworld.jpg\t/docs/world.jpg\t%s\t%d" hostname port)
             (lines 1)))
      (is (= "." (lines 2))))))

(deftest not-found
  (testing "directory not found"
    (is (nil? (get-contents base-dir "./abc"))))

  (testing "file not found"
    (is (nil? (get-contents base-dir "abc.txt")))))

(defn- stream->bytes
  [in]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (map byte (.toByteArray out))))

(deftest transfer
  (testing "binary"
    (with-open [r (io/input-stream (io/file base-dir "docs" "world.jpg"))
                r' (get-contents base-dir "docs/world.jpg")]
      (is (= (stream->bytes r) (stream->bytes r')))))

  (testing "text"
    (let [lines (s/split-lines (slurp
                                (io/file base-dir "docs" "hello.txt")))
          lines' (s/split-lines (roundtrip "docs/hello.txt"))]
      (is (= 3 (count lines)))
      (is (= 4 (count lines')))
      (is (= lines (take 3 lines')))
      (is (= "." (last lines'))))))