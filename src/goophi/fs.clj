(ns goophi.fs
  (:require [goophi.core :as core]
            [goophi.config :as config]
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
  (config/bind [^{:default {}} extensions [:file-extensions]]
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
  [file]
  (if (.isFile file)
    (guess-file-type (.getName file))
    "1"))

(defn file->item
  [file]
  (config/bind [^:required hostname [:network :hostname]
                ^{:default 70} port [:network :port]]
    (core/->Item (map-file-type file)
                 (.getName file)
                 (str "/" (.getName file))
                 hostname
                 port)))

(defn- list-directory
  [dir]
  (->> (.listFiles dir)
       rest
       (map (comp str file->item))
       s/join))

(defn- replace-keyword
  [text]
  (config/bind [^:required hostname [:network :hostname]
                ^{:default 70} port [:network :port]]
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
         (s/join))))

(defn- read-directory
  [dir]
  (let [gophermap (io/file dir "gophermap")]
    (if (.exists gophermap)
      (read-gophermap gophermap)
      (list-directory dir))))

(defn- read-file
  [file]
  (FileInputStream. file))

(defn- ->Path
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
        (.isDirectory file) (read-directory file)
        (.isFile file) (read-file file))
      (core/error "Access denied."))))
