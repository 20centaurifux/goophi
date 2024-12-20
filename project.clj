(defproject de.dixieflatline/goophi "0.2.0"
  :description "Gopher protocol library."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.1.266"]
                 [instaparse "1.5.0"]
                 [hiccup "1.0.5"]
                 [manifold "0.4.3"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :aot nil
  :profiles {:test {:dependencies [["aleph" "0.8.1"]]}}
  :plugins [[org.clojure/test.check "1.1.1"]
            [dev.weavejester/lein-cljfmt "0.13.0"]
            [lein-codox "0.10.8"]]
  :cljfmt {:load-config-file? true}
  :codox {:output-path "./doc"})
