(ns goophi.response-test
  (:require [clojure.string :as s]
            [clojure.test :refer [deftest testing is]]
            [goophi.core :refer [info]]
            [goophi.response :as rsp]
            [goophi.test-utils :as utils]))

(defn- str->stream
  [text]
  (-> text
      .getBytes
      java.io.ByteArrayInputStream.))

(deftest is-input-stream?
  (testing "menu entity"
    (is (instance? java.io.InputStream
                   (rsp/menu-entity "hello world"))))

  (testing "text-file entity"
    (is (instance? java.io.InputStream
                   (rsp/text-file-entity (str->stream "hello world")))))

  (testing "binary entity"
    (is (instance? java.io.InputStream
                   (rsp/binary-entity (byte-array [1 3 3 7]))))))

(deftest print-stream
  (testing "string"
    (let [dump (-> (str->stream "hello world")
                   rsp/print-text-stream
                   with-out-str
                   s/trim)]
      (is (= "hello world" dump))))

  (testing "hex"
    (let [dump (-> (str->stream "hello world")
                   rsp/print-binary-stream
                   with-out-str
                   s/trim)]
      (is (= "0x68 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64" dump)))))

(deftest binary-files
  (testing "binary entity"
    (with-open [e (utils/open-binary-file)
                e' (rsp/binary-entity (utils/open-binary-file))]
      (is (= (-> (rsp/print-binary-stream e) with-out-str)
             (-> (rsp/print-binary-stream e') with-out-str))))))

(deftest text-files
  (testing "append last line"
    (let [lines (-> (str->stream "hello world")
                    rsp/text-file-entity
                    rsp/print-text-stream
                    with-out-str
                    s/split-lines)]
      (is (= 2 (count lines)))
      (is (= "hello world" (first lines)))
      (is (= "." (second lines))))

    (testing "prepend extra period"
      (let [lines (-> (str->stream "hello.\r\n.\r\nwor.ld")
                      rsp/text-file-entity
                      rsp/print-text-stream
                      with-out-str
                      s/split-lines)]
        (is (= 4 (count lines)))
        (is (= "hello." (lines 0)))
        (is (= ".." (lines 1)))
        (is (= "wor.ld" (lines 2)))
        (is (= "." (lines 3)))))

    (testing "strip extra periods"
      (let [lines (-> (str->stream "hello.\r\n...\r\nwor.ld")
                      rsp/text-file-entity
                      rsp/print-text-stream
                      with-out-str
                      s/split-lines)]
        (is (= 4 (count lines)))
        (is (= "hello." (lines 0)))
        (is (= ".." (lines 1)))
        (is (= "wor.ld" (lines 2)))
        (is (= "." (lines 3)))))

    (testing "preserve line break"
      (let [lines (-> (str->stream "hello\r\n\r\n\r\nworld")
                      rsp/text-file-entity
                      rsp/print-text-stream
                      with-out-str
                      s/split-lines)]
        (is (= 5 (count lines)))
        (is (= "hello" (lines 0)))
        (is (= "" (lines 1)))
        (is (= "" (lines 2)))
        (is (= "world" (lines 3)))
        (is (= "." (lines 4)))))

    (testing "replace tab"
      (let [lines (-> (str->stream "hello\tworld")
                      rsp/text-file-entity
                      rsp/print-text-stream
                      with-out-str
                      s/split-lines)]
        (is (= 2 (count lines)))
        (is (not (.contains (first lines) "\t")))
        (is (some? (re-matches #"hello\s+world" (first lines))))
        (is (= "." (second lines)))))

    (testing "slurp"
      (let [lines (-> (str->stream "hello world")
                      rsp/text-file-entity
                      slurp
                      s/split-lines)]
        (is (= 2 (count lines)))
        (is (= "hello world" (first lines)))
        (is (= "." (second lines)))))))

(deftest menu-entities
  (testing "from string"
    (let [lines (-> "hello world"
                    rsp/menu-entity
                    rsp/print-text-stream
                    with-out-str
                    s/split-lines)]
      (is (= 2 (count lines)))
      (is (= "hello world" (first lines)))
      (is (= "." (second lines)))))

  (testing "from item"
    (let [lines (-> (info "hello world")
                    rsp/menu-entity
                    rsp/print-text-stream
                    with-out-str
                    s/split-lines)]
      (is (= 2 (count lines)))
      (is (= "ihello world\tfake\t(NULL)\t0" (first lines)))
      (is (= "." (second lines))))))