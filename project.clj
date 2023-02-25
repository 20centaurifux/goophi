(defproject zcfux/goophi "0.1.3"
  :description "Gopher protocol library."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse "1.4.12"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/core.memoize "1.0.257"]
                 [hiccup "1.0.5"]
                 [zcfux/confick "0.1.4"]
                 [manifold "0.3.0"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [["aleph" "0.6.0"]]}}
  :plugins [[lein-cljfmt "0.9.2"]
            [lein-marginalia "0.9.1"]]
  :cljfmt {:indents {bind [[:inner 0]]}})
