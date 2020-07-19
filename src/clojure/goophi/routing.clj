(ns goophi.routing
  (:require [goophi.core :refer [parse-request]]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(defonce ^:private route-parser
  (insta/parser
   (str
    "route    = part*
     <part>   = literal | escaped | wildcard | param
     literal  = #'(:[^\\pL_*{}\\\\]|[^:*{}\\\\])+'
     escaped  = #'\\\\.'
     wildcard = '*'
     param    = <':'> #'([\\pL_][\\pL\\pN-_]*)'")))

(defonce ^:private re-chars (set "\\.*+|?()[]{}$^"))

(defn- re-escape [s]
  (s/escape s #(if (re-chars %) (str \\ %))))

(defn- tree->regex
  [route]
  (insta/transform
   {:route   (comp re-pattern str)
    :literal re-escape
    :escaped #(re-escape (subs % 1))
    :wildcard (constantly "(.*?)")
    :param (partial format "(?<%s>[\\u0020-\\u002e\\u0030-\\u007e]+)")}
   route))

(defn- params
  [tree]
  (->> (rest tree)
       (filter #(= (first %) :param))
       (map (comp keyword second))))

(defrecord Pattern [tree regex params])

(defn compile-pattern
  "Compiles a pattern. Patterns may consist of literals, escaped
  characters, wildcards (*) and parameters (:name). The syntax is
  very similar to to Clout (https://github.com/weavejester/clout).
  That's no surprise, because the code is similar too :)"
  [pattern]
  (let [tree (insta/parse route-parser pattern)
        regex (tree->regex tree)
        params (params tree)]
    (Pattern. tree regex params)))

(defn matches
  "Tests if request is matching pattern. Returns a map containing
  path, query and matched parameters on success."
  [route request]
  (when-let [request' (parse-request request)]
    (when-let [match (re-matches (:regex route) (first request'))]
      {:path (first request')
       :query (second request')
       :params (into (sorted-map) (zipmap (:params route) (rest match)))})))

(defn- split-vars
  ([vars]
   (split-vars [] [] (vec vars)))
  ([param-bindings req-bindings vars]
   (if-let [top (first vars)]
     (if (= top :as)
       (split-vars param-bindings (conj req-bindings (second vars)) (drop 2 vars))
       (split-vars (conj param-bindings top) req-bindings (rest vars)))
     (list param-bindings req-bindings))))

(defmacro route
  "Returns a function that takes a request as argument. The function
  evaluates body and returns the value of the expression if pattern
  is matching the request path. Parameters are bound to vars.

  Example:
    (route \"/products/:category/:name\"
           [category name]
           (get-product category name))

  Use the :as keyword to assign the entire request map to a symbol:

  Example:
    (route \"/products/:category/search\"
           [category :as req]
           (search-products category (:query req)))"
  [pattern vars body]
  (let [r# (compile-pattern pattern)
        [param-bindings# req-bindings#] (split-vars vars)]
    `(fn [request#]
       (when-let [match# (matches ~r# request#)]
         (let [~param-bindings# (take ~(count param-bindings#) (vals (:params match#)))
               ~req-bindings# (repeat ~(count req-bindings#) match#)] ~body)))))

(defmacro defroutes
  "Defines a set of routes.

  Example:
    (defroutes my-routes
      (\"/products/:category/:id\"
       [category id]
       (get-product category id))

      (\"/products/:category/search\"
       [category :as req]
       (search-products category (:query req))))"
  [name & routes]
  `(def ~name ~(mapv #(cons `route %) routes)))
