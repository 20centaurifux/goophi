(ns goophi.config
  (:require [clojure.edn :as edn]
            [clojure.core.memoize :as memo]
            [clojure.string :as s]))

(def ^:private timeout 30000)

(def ^:private load-config
  (memo/ttl #(-> (slurp "config.edn")
                 edn/read-string)
            :ttl/threshold timeout))

(defn- ->keys
  [ks]
  (flatten [ks]))

(defn- lookup
  [m ks]
  (if-some [v (or (get-in (load-config) (->keys ks))
                  (:default m))]
    v
    (when (:required m) (throw (Exception. (format "Key %s not found."
                                                   (s/join "::"
                                                           (->keys ks))))))))

(defmacro bind
  "Evaluates body in a lexical scope in which the symbols in the
  binding-forms are bound to their corresponding configuration
  values.

  Example:
    (bind [addr [:tcp :address]
           port [:tcp :port]]
      (format \"%s:%d\" addr port))

  Use metadata to assign default values or make configuration keys
  mandatory.

  Example:
    (bind [^:required addr [:tcp :address]
           ^{default: 70} port [:tcp :port]]
      (format \"%s:%d\" addr port))

  Configuration is loaded from an EDN formatted file named
  \"config.edn\" which must be located in the working directory of
  the process. File content is cached."
  [bindings & body]
  `(let* ~(vec (mapcat #(list (first %)
                              (lookup (meta (first %)) (second %)))
                       (partition 2 bindings)))
         ~@body))
