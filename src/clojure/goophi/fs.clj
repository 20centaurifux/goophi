(ns goophi.fs
  (:require [goophi.core :as core]
            [goophi.response :as response]
            [confick.core :as config]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:import [java.net URLConnection]
           [java.io FileInputStream]
           [java.nio.file Paths]))

(defn- file-extension
  [filename]
  (-> (s/split filename #"\.")
      last
      s/lower-case))

(defn- file-extension-map
  []
  (config/bind [^{:default {}} extensions [:goophi :fs :file-extensions]]
    (-> extensions
        (update "g" #(conj % "gif"))
        (update "0" #(conj % "xml" "json")))))

(defn- map-extension
  [filename]
  (let [ext (file-extension filename)]
    (some (fn [[t e]] (when (some #(= ext %) e) t))
          (file-extension-map))))

(defn- map-mime
  [filename]
  (let [mime (or (URLConnection/guessContentTypeFromName filename)
                 "")]
    (cond
      (s/starts-with? mime "text/") "0"
      (s/starts-with? mime "image/") "i")))

(defn- guess-file-type
  [filename]
  (or (map-extension filename)
      (map-mime filename)
      "9"))

(defn- map-file-type
  [^java.io.File file]
  (if (.isFile file)
    (guess-file-type (.getName file))
    "1"))

(defn- ->selector
  [parent filename]
  (let [rtrimmed (s/replace parent #"/$" "")
        selector (str rtrimmed "/" filename)]
    (cond->> selector
      (not (s/starts-with? selector "/")) (str "/"))))

(defn- file->item
  [parent ^java.io.File file]
  (config/bind [^:required hostname [:goophi :hostname]
                ^{:default 70} port [:goophi :port]]
    (core/->Item (map-file-type file)
                 (.getName file)
                 (->selector parent (.getName file))
                 hostname
                 port)))

(defn- list-directory
  [selector ^java.io.File dir]
  (->> (.listFiles dir)
       sort
       (map #(str (file->item selector %)))
       s/join
       response/menu-entity))

(defn- replace-keyword
  [text]
  (config/bind [^:required hostname [:goophi :hostname]
                ^{:default 70} port [:goophi :port]]
    (cond
      (= text "@hostname") hostname
      (= text "@port") port
      :else text)))

(defn- transform-gophermap-parts
  [parts]
  (concat (take 3 parts)
          (map replace-keyword (take-last 2 parts))))

(defn- convert-line
  [line]
  (if-let [match (re-matches #"^([a-zA-Z0-9])(.+)\t(.+)\t(.+)\t(\d+|@port)$" (s/trim line))]
    (apply core/->Item (transform-gophermap-parts (rest match)))
    (core/info line)))

(defn- read-gophermap
  [file]
  (with-open [rdr (io/reader file)]
    (->> (line-seq rdr)
         (map (comp str convert-line))
         s/join
         response/menu-entity)))

(defn- read-directory
  [selector dir]
  (let [gophermap (io/file dir "gophermap")]
    (if (.exists gophermap)
      (read-gophermap gophermap)
      (list-directory selector dir))))

(defn- read-file
  [^java.io.File file]
  (let [in (FileInputStream. file)]
    (if (= "0" (guess-file-type (.getPath file)))
      (response/text-file-entity in)
      (response/binary-entity in))))

(defn- ^java.nio.file.Path ->Path
  [path]
  (.normalize
   (-> (.toURI (io/file path))
       Paths/get)))

(defn- is-child-path?
  [parent child]
  (let [parent' (->Path parent)
        child' (->Path child)]
    (and (>= (.getNameCount child') (.getNameCount parent'))
         (.startsWith child' parent'))))

(defn- normalize-relative-path
  [path]
  (cond
    (nil? path) ""
    (s/starts-with? path "/") (str "." path)
    :else path))

(defn get-contents
  "Returns a gopher menu or file stream."
  [base-dir path]
  (let [file (io/file base-dir (normalize-relative-path path))]
    (if (is-child-path? base-dir (.getPath file))
      (cond
        (.isDirectory file) (read-directory (or path "") file)
        (.isFile file) (read-file file))
      (response/menu-entity (core/info "Access denied.")))))
