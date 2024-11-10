(defproject zcfux/goophi "0.1.4-SNAPSHOT"
  :description "Gopher protocol library."
  :url "https://github.com/20centaurifux/goophi"
  :license {:name "AGPLv3"
            :url "https://www.gnu.org/licenses/agpl-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse "1.5.0"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/core.memoize "1.1.266"]
                 [hiccup "1.0.5"]
                 [de.dixieflatline/confick "0.2.0-SNAPSHOT"]
                 [manifold "0.4.3"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :target-path "target/%s"
  :aot nil
  :profiles {:test {:dependencies [["aleph" "0.8.1"]]}}
  :plugins [[dev.weavejester/lein-cljfmt "0.13.0"]
            [lein-marginalia "0.9.2"]]
  :cljfmt {:load-config-file? true})