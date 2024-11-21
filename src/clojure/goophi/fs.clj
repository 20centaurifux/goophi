(ns goophi.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [goophi.core :as goo]
            [goophi.response :as rsp])
  (:import java.io.FileInputStream
           java.net.URLConnection
           java.nio.file.Paths))

(defn- file-extension
  [filename]
  (some-> (re-matches #"(?i).*[^/]\.([a-z0-9]+)$" filename)
          last
          s/lower-case))

(defn- map-extension
  [filename item-type-map]
  (let [ext (file-extension filename)]
    (some (fn [[t coll]]
            (when (some #(= ext %) coll) t))
          item-type-map)))

(defn- map-mime
  [filename]
  (let [mime (or (URLConnection/guessContentTypeFromName filename) "")]
    (cond
      (s/starts-with? mime "text/") "0"
      (s/starts-with? mime "image/") "i")))

(defn- guess-file-type
  [filename item-type-map]
  (or (map-extension filename item-type-map)
      (map-mime filename)
      "9"))

(defn- map-file-type
  [^java.io.File file item-type-map]
  (if (.isFile file)
    (guess-file-type (.getName file) item-type-map)
    "1"))

(defn- ->selector
  [parent filename]
  (let [rtrimmed (s/replace parent #"/$" "")
        selector (str rtrimmed "/" filename)]
    (cond->> selector
      (not (s/starts-with? selector "/")) (str "/"))))

(defn- file->item
  [parent ^java.io.File file hostname port item-type-map]
  (goo/->Item (map-file-type file item-type-map)
              (.getName file)
              (->selector parent (.getName file))
              hostname
              port))

(defn- list-directory
  [selector ^java.io.File dir hostname port item-type-map]
  (->> (.listFiles dir)
       sort
       (map #(str (file->item selector % hostname port item-type-map)))
       s/join
       rsp/menu-entity))

(defn- transform-gophermap-parts
  [parts keywords]
  (concat (take 3 parts)
          (map #(get keywords % %) (take-last 2 parts))))

(defn- convert-line
  [line keywords]
  (if-let [match (re-matches #"^(?i)([a-z0-9\+:;<])(.+)\t(.+)\t(.+)\t(\d+|@port)$"
                             (s/trim line))]
    (apply goo/->Item (transform-gophermap-parts (rest match) keywords))
    (goo/info line)))

(defn- read-gophermap
  [file hostname port]
  (with-open [rdr (io/reader file)]
    (->> (line-seq rdr)
         (map (comp str #(convert-line % {"@hostname" hostname "@port" port})))
         s/join
         rsp/menu-entity)))

(defn- read-directory
  [selector dir hostname port item-type-map]
  (let [gophermap (io/file dir "gophermap")]
    (if (.exists gophermap)
      (read-gophermap gophermap hostname port)
      (list-directory selector dir hostname port item-type-map))))

(defn- read-file
  [^java.io.File file item-type-map]
  (let [in (FileInputStream. file)]
    (if (= "0" (guess-file-type (.getPath file) item-type-map))
      (rsp/text-file-entity in)
      (rsp/binary-entity in))))

(defn- ->Path
  ^java.nio.file.Path [path]
  (.normalize
   (-> (.toURI (io/file path))
       Paths/get)))

(defn- is-child-path?
  [parent child]
  (let [parent' (->Path parent)
        child' (->Path child)]
    (and (>= (.getNameCount child') (.getNameCount parent'))
         (.startsWith child' parent'))))

(defn- normalize-path
  [path]
  (-> (or path "")
      (s/replace #"^/" "./")
      (s/replace #"gophermap$" "")))

(defn get-contents
  "Returns a gopher menu or file stream."
  [base-dir path & {:keys [hostname port item-type-map]
                    :or {hostname "localhost" port 70 item-type-map {}}}]
  (let [file (io/file base-dir (normalize-path path))]
    (if (is-child-path? base-dir (.getPath file))
      (cond
        (.isDirectory file) (read-directory path file hostname port item-type-map)
        (.isFile file) (read-file file item-type-map))
      (rsp/menu-entity (goo/info "Access denied.")))))