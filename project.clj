(defproject goophi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse "1.4.10"]
                 [aleph "0.4.7-alpha5"]
                 [org.clojure/core.async "1.2.603"]
                 [org.clojure/core.memoize "1.0.236"]]
  :main ^:skip-aot goophi.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-cljfmt "0.6.7"]])
