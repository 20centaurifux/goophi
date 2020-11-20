(ns goophi.redirect
  (:use hiccup.core hiccup.page))

(defn- page
  [url]
  (html5 {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:meta {:http-equiv "Refresh"
                  :content (str "1; URL=" url)}]
          [:title "Redirect"]]
         [:body
          "Redirecting to "
          [:a {:href url} url]]))

(defn redirect
  [url]
  (java.io.ByteArrayInputStream. (.getBytes (page url))))

(defn selector->url
  [selector]
  (let [url (subs selector 4)]
    (when-not (clojure.string/blank? url)
      (try
        (clojure.java.io/as-url url)
        (catch Exception _)))))
