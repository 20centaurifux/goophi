(defproject zcfux/goophi "0.1.0-SNAPSHOT"
  :description "Gopher protocol library."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse "1.4.10"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/core.memoize "1.0.236"]
                 [hiccup "1.0.5"]
                 [zcfux/confick "0.1.0-SNAPSHOT"]
                 [manifold "0.1.9-alpha4"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-cljfmt "0.6.7"]])
