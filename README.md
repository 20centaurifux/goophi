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

The routing module converts a Gopher request to a map & evaluates a function returning an entity.

	(require '[goophi.core :as c]
	         '[goophi.routing :as r]
	         '[goophi.response :as res])

	(def hello-world
	  (r/route
	    "*"
	    []
	    (res/menu-entity (c/info "hello world"))))

	(res/dumps (hello-world ""))

	-> ihello world    fake    (NULL)  0
	-> .

### filesystem module

gooφ has a built-in filesystem module with gophermap support.

	(require '[goophi.fs :as fs])

	(def fs-example
	  (r/route
	    "*"
	    [:as req]
	    (fs/get-contents "./pub" (:path req))))

	(res/dumps (fs-example "helloworld.txt"))

	->   |\__/,|   (`\
	->  _.|o o  |_   ) )
	-> -(((---(((--------
	-> .

Hostname and port are specified in the configuration file (config.edn).
gooφ uses [confick](https://github.com/20centaurifux/confick) for configuration
management.

### redirection module

URLs are displayed on an HTML redirection page.

	(require '[goophi.redirect :as html])

	(def redirect-example
	  (r/route
	    "URL\\:*"
	    [:as req]
	    (if-let [url (html/selector->url (:path req))]
	      (html/redirect url)
	      (res/menu-entity (c/info "Not found.")))))

	(res/dumps (redirect-example "URL:https://github.com/20centaurifux/goophi"))

### TCP

Build Aleph compatible request handlers with the tcp module.

	(require '[aleph.tcp :as tcp]
	         '[goophi.tcp :refer [->gopher-handler]])
 
	(r/defroutes routes
	   ("*"
	     [:as req]
	     (fs/get-contents "./pub" (:path req))))

	(tcp/start-server
	 (->gopher-handler routes)
	 {:port 70})
