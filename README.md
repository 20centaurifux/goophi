# gooφ

gooφ is a Gopher implementation written in Clojure. It comes with the following features:

* self-explanatory & powerful routing syntax
* built-in filesystem module
* built-in HTTP redirection module
* [Aleph](https://github.com/aleph-io/aleph) compatibility

## Installation

The library can be installed from Clojars using Leiningen:

[![Clojars Project](http://clojars.org/zcfux/goophi/latest-version.svg)](https://clojars.org/zcfux/goophi)

## Quick overview

### Routing & entities

A Gopher request is represented as a map. It has the following keys:

* path: selector
* query: search string
* params: parameters found in selector
* remote-addr: remote ip address

The routing module converts a Gopher request to a map & evaluates a function returning an entity.

	(use 'goophi.core)
	(use 'goophi.routing)
	(use 'goophi.response)

	(def hello-world
	  (->route
	   "*"
	   []
	   (menu-entity (info "hello world"))))

	(dumps (hello-world ""))

	-> ihello world    fake    (NULL)  0
	-> .

### filesystem module

gooφ has a built-in filesystem module with gophermap support.

	(use 'goophi.fs)

	(def fs-example
	  (->route
	   "*"
	   [:as req]
	   (get-contents "./example-pub" (:path req))))

	(dumps (fs-example "docs/hello.txt"))

	->   |\__/,|   (`\
	->  _.|o o  |_   ) )
	-> -(((---(((--------
	-> .

Hostname and port are specified in the configuration file (config.edn).
gooφ uses [confick](https://github.com/20centaurifux/confick) for configuration
management.

### redirection module

URLs are displayed on an HTML redirection page.

	(use 'goophi.redirect)

	(def redirect-example
	  (->route
	   "URL\\:*"
	   [:as req]
	   (if-let [url (selector->url (:path req))]
	     (redirect url)
	     (menu-entity (info "Not found.")))))

	(dumps (redirect-example "URL:https://github.com/20centaurifux/goophi"))

### TCP

Build Aleph compatible request handlers with the tcp module.

	(require '[aleph.tcp :as tcp]
	         '[goophi.tcp :refer [->gopher-handler]])

	(def my-routes
	  (->routes
	   ("*"
	   [:as req]
	   (get-contents "./example-pub" (:path req)))))

	(tcp/start-server
	 (->gopher-handler my-routes)
	 {:port 8070})

## Middleware

Read or change the request map by composing a custom request handler.

	(defn log-request
	  [request]
	  (printf "address: %s, path: %s\n"
	          (:remote-addr request)
	          (:path request))
	  (flush)
	  request) ; bypass request map

	(def my-app
	  (comp
	   (->routes
	    ("*"
	     [:as req]
	     (get-contents "./example-pub" (:path req))))
	   log-request))

	(def s (tcp/start-server
	        (->gopher-handler my-app)
	        {:port 8070}))
