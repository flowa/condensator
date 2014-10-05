(defproject condensator "0.1.0-SNAPSHOT"
  :description "condensator clojure library"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojurewerkz/meltdown "1.0.0"]
                 [com.taoensso/timbre "3.2.1"]
                 [org.projectreactor/reactor-net "1.1.4.RELEASE"] 
                 [org.projectreactor/reactor-core "1.1.4.RELEASE"] ]
  :repositories { "sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases  {:checksum :fail}}}
  :profiles  {:dev  {:dependencies  [[speclj "3.1.0"]]}}
  :plugins  [[speclj "3.1.0"]]
  :test-paths  ["test"])
