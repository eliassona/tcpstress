(ns tcpstress.core
  (:require [clojure.java.shell :refer [sh]])
  (:import [java.io File]))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))
(def mz-home (File. (System/getenv "MZ_HOME")))



(def auto-test-home (File. (-> mz-home .getParentFile .getParentFile .getParentFile) "autotest"))


(defn run-test [the-test]
  (sh "mzsh" 
      "mzadmin/dr" 
      "script" 
      "test" 
      (format "scripts/tests/%s" the-test)  
      :dir auto-test-home))


(defn test-until-fail [the-test]
  (loop [i 1]
    (let [res (time (run-test the-test))
          text (:out res)]
      (if (< (.indexOf text "fail") 0)
        (do 
          (println i)
          (recur (inc i)))
          (println text)))))


