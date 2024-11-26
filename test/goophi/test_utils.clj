(ns goophi.test-utils
  (:require [clojure.java.io :as io]))

(defonce ^:private base-dir "./example-pub")

(defn stream->byte-array
  [in]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn open-binary-file
  []
  (-> (io/file base-dir "docs" "world.jpg")
      io/input-stream))

(defn open-text-file
  []
  (-> (io/file base-dir "docs" "hello.txt")
      io/input-stream))