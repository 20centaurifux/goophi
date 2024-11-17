(ns goophi.fs-test
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.test :refer [deftest testing is]]
            [goophi.fs :refer [get-contents]]
            [goophi.response :as rsp]))

(defonce ^:private base-dir "./example-pub")
(defonce ^:private hostname "example.org")
(defonce ^:private port 7070)

(defn- roundtrip
  [selector]
  (-> (get-contents base-dir selector :hostname hostname :port port)
      rsp/dumps
      with-out-str))

(deftest directory-traversal
  (testing "directory traversal"
    (is (some? (re-matches #"(?is)iaccess denied.*" (roundtrip "../"))))))

(deftest menu
  (testing "gophermap"
    (doseq [selector ["" "gophermap"]]
      (let [lines (s/split (roundtrip selector) #"\r\n")]
        (is (= 4 (count lines)))
        (is (= "iWelcome!\tfake\t(NULL)\t0" (nth lines 0)))
        (is (= "i\tfake\t(NULL)\t0" (nth lines 1)))
        (is (= (format "1docs\tdocs\t%s\t%d" hostname port)
               (nth lines 2)))
        (is (= "." (nth lines 3))))))

  (testing "listing"
    (let [lines (s/split (roundtrip "docs") #"\r\n")]
      (is (= 3 (count lines)))
      (is (= (format "0hello.txt\t/docs/hello.txt\t%s\t%d" hostname port)
             (nth lines 0)))
      (is (= (format "iworld.jpg\t/docs/world.jpg\t%s\t%d" hostname port)
             (nth lines 1)))
      (is (= "." (nth lines 2))))))

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
    (let [lines (s/split (slurp
                          (io/file base-dir "docs" "hello.txt"))
                         #"\n")
          lines' (s/split (roundtrip "docs/hello.txt")
                          #"\r\n")]
      (is (= 3 (count lines)))
      (is (= 4 (count lines')))
      (is (= lines (take 3 lines')))
      (is (= "." (last lines'))))))