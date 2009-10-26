(println (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader))))

(.printStackTrace *e)
(clojure.stracktrace/print-cause-trace)
(doc *e)
(agent-errors my-agent)
(clear-agent-errors my-agent)
;;emacs C-c C-k
