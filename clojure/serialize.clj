;; http://stackoverflow.com/questions/1288877/what-would-be-the-correct-way-to-serialize-this-java-object-in-clojure

(use 'clojure.contrib.duck-streams)

(defn serialize [o filename]
  (binding [*print-dup* true]
   (with-out-writer filename
     (prn o))))

(defn deserialize [filename]
  (read-string (slurp* filename)))

;; Example:

;; user> (def box {:height 50 :width 20})
;; #'user/box
;; user> (serialize box "foo.ser")
;; nil
;; user> (deserialize "foo.ser")
;; {:height 50, :width 20}
;; This works for most Clojure objects already, but fails for most Java objects.

;; user> (serialize (java.util.Date.) "date.ser")
;; ; Evaluation aborted.
;; No method in multimethod 'print-dup' for dispatch value: class java.util.Date
;; But you can add methods to the print-dup multimethod to allow Clojure to print other objects readably.

;; user> (defmethod clojure.core/print-dup java.util.Date [o w]
;;         (.write w (str "#=(java.util.Date. " (.getTime o) ")")))
;; #<MultiFn clojure.lang.MultiFn@1af9e98>
;; user> (serialize (java.util.Date.) "date.ser")
;; nil
;; user> (deserialize "date.ser")
;; #<Date Mon Aug 17 11:30:00 PDT 2009>
;; If you have a Java object with a native Java serialization method, you can just use that and not bother writing your own code to do it.