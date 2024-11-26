# gooφ

gooφ is a Gopher implementation written in Clojure. It comes with the following features:

* self-explanatory & powerful routing syntax
* built-in filesystem module
* built-in HTTP redirection module
* [Aleph](https://github.com/aleph-io/aleph) compatibility

## Installation

The library can be installed from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/de.dixieflatline/goophi.svg?include_prereleases)](https://clojars.org/de.dixieflatline/goophi)

## Quick overview

### Routing & entities

A Gopher request is represented as a map. It has the following keys:

* path: selector
* query: search string
* params: parameters found in selector
* remote-addr: remote ip address (added by the Aleph handler)

The routing module checks whether a path from the request map matches a pattern and evaluates the corresponding body upon success.

	(use 'goophi.core)
	(use 'goophi.routing)
	(use 'goophi.response)

	;; construct request map for testing purposes
	(defn- ->request
	  [path]
	  {:path path})

	(def hello-world
	  (route
	   "*"
	   []
	   (menu-entity (info "hello world"))))

	(print-text-stream (hello-world (->request "/")))

	-> ihello world	fake	(NULL)	0
	-> .

Placeholders can also be used, which are bound to vars.

	(def route-with-vars
	  (route
	   "/manga/articles/:category"
	   [category]
	   (menu-entity (info (str "Articles from category " category)))))

	(print-text-stream (route-with-vars (->request "/blog/articles/programming")))

	-> iArticles from category programming	fake	(NULL)	0
	-> .

### filesystem module

gooφ has a built-in filesystem module with gophermap support.

	(use 'goophi.fs)

	(def fs-example
	  (route
	   "*"
	   [:as req]
	   (get-contents "./example-pub" (:path req))))

	(print-text-stream (fs-example (->request "docs/hello.txt")))

	->   |\__/,|   (`\
	->  _.|o o  |_   ) )
	-> -(((---(((--------
	-> .

### redirection module

URLs are displayed on an HTML redirection page.

	(use 'goophi.redirect)

	(def redirect-example
	  (route
	   "URL\\:*"
	   [:as req]
	   (if-let [url (selector->url (:path req))]
	     (redirect url)
	     (menu-entity (info "Not found.")))))

	(print-text-stream
	 (redirect-example (->request "URL:https://dixieflatline.de")))

	-> <!DOCTYPE html> ...

### TCP

Build Aleph compatible request handlers with the tcp module.

	(require '[aleph.tcp :as tcp]
	         '[goophi.tcp :refer [aleph-handler]])

	(def my-routes
	  (routes
	   ("*"
	   [:as req]
	   (or (get-contents "./example-pub" (:path req))
	       (menu-entity (info "Not found."))))))

	(tcp/start-server
	 (aleph-handler my-routes)
	 {:port 70})

## Middleware

Read or change the request map by composing a custom request handler.

	(defn log-request
	  [handler]
	  (fn [req]
	    (printf "address: %s, path: %s\n"
	            (:remote-addr req)
	            (:path req))
	    (flush)
	    (handler req)))

	(def my-app
	  (-> my-routes
	      log-request))

	(def s (tcp/start-server
	        (aleph-handler my-app)
	        {:port 70}))