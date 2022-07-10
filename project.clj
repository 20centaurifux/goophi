(defproject zcfux/goophi "0.1.1"
  :description "Gopher protocol library."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse "1.4.12"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/core.memoize "1.0.257"]
                 [hiccup "1.0.5"]
                 [zcfux/confick "0.1.2"]
                 [manifold "0.2.4"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [["aleph" "0.4.6"]]}}
  :plugins [[lein-cljfmt "0.8.2"]
            [lein-marginalia "0.9.1"]]
  :cljfmt {:indents {bind [[:inner 0]]}})
