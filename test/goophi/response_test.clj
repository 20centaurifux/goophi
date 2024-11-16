(ns goophi.response-test
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.test :refer [deftest testing is]]
            [goophi.core :refer [info]]
            [goophi.response :refer [binary-entity
                                     dumps
                                     dumpx
                                     menu-entity
                                     Response?
                                     text-file-entity]]))

(defonce ^:private base-dir "./example-pub")

(defn- str->stream
  [text]
  (-> text
      .getBytes
      java.io.ByteArrayInputStream.))

(deftest is-response?
  (testing "no stream"
    (is (not (Response? "hello world")))
    (is (not (Response? 42)))
    (is (not (Response? {:foo "bar"}))))

  (testing "input-stream"
    (is (Response? (str->stream "hello world"))))

  (testing "menu entity"
    (is (Response? (menu-entity "hello world"))))

  (testing "text-file entity"
    (is (Response? (text-file-entity (str->stream "hello world")))))

  (testing "binary entity"
    (is (Response? (binary-entity (byte-array [1 3 3 7]))))))

(deftest dump
  (testing "string"
    (let [dump (-> (str->stream "hello world")
                   dumps
                   with-out-str
                   s/trim)]
      (is (= "hello world" dump))))

  (testing "hex"
    (let [dump (-> (str->stream "hello world")
                   dumpx
                   with-out-str
                   s/trim)]
      (is (= "0x68 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64" dump)))))

(deftest binary-files
  (testing "binary entity"
    (with-open [e (io/input-stream (io/file base-dir "docs" "world.jpg"))
                e' (binary-entity e)]
      (= (-> (dumpx e) with-out-str)
         (-> (dumpx e') with-out-str)))))

(deftest text-files
  (testing "last line"
    (let [lines (-> (str->stream "hello world")
                    text-file-entity
                    dumps
                    with-out-str
                    (s/split #"\r\n"))]
      (is (= 2 (count lines)))
      (is (= "hello world" (first lines)))
      (is (= "." (second lines))))

    (testing "replace dot prefix"
      (let [lines (-> (str->stream "hello.\r\n.\r\nwor.ld")
                      text-file-entity
                      dumps
                      with-out-str
                      (s/split #"\r\n"))]
        (is (= 4 (count lines)))
        (is (= "hello." (nth lines 0)))
        (is (= ".." (nth lines 1)))
        (is (= "wor.ld" (nth lines 2)))
        (is (= "." (nth lines 3)))))

    (testing "replace tab"
      (let [lines (-> (str->stream "hello\tworld")
                      text-file-entity
                      dumps
                      with-out-str
                      (s/split #"\r\n"))]
        (is (= 2 (count lines)))
        (is (not (.contains (first lines) "\t")))
        (is (some? (re-matches #"hello\s+world" (first lines))))
        (is (= "." (second lines)))))))

(deftest menu-entities
  (testing "from string"
    (let [lines (-> "hello world"
                    menu-entity
                    dumps
                    with-out-str
                    (s/split #"\r\n"))]
      (is (= 2 (count lines)))
      (is (= "hello world" (first lines)))
      (is (= "." (second lines)))))

  (testing "from item"
    (let [lines (-> (info "hello world")
                    menu-entity
                    dumps
                    with-out-str
                    (s/split #"\r\n"))]
      (is (= 2 (count lines)))
      (is (= "ihello world\tfake\t(NULL)\t0" (first lines)))
      (is (= "." (second lines))))))