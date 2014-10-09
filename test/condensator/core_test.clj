(ns condensator.core-test
  (:use [speclj.core])
  (:require [condensator.core :as condensator]))

(describe "Create tests"
          (it "Creates instance of Reactorish object" 
              (should= (type (condensator/create)) reactor.core.Reactor)))

(run-specs)
