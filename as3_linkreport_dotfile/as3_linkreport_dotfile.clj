(require
 '[clojure.zip :as zip]
 '[clojure.xml :as xml]
 '[clojure.contrib.pprint :as pp]
 '[clojure.contrib.zip-filter :as zf]
 '[clojure.contrib.zip-filter.xml :as zfx]
 '[clojure.contrib.duck-streams :as ds]
 '[com.arcanearcade.clojure.utils.file-utils :as fu])

(defn parse-str [s]
  (zip/xml-zip (xml/parse (new org.xml.sax.InputSource
                               (new java.io.StringReader s)))))
(defn process-report [file]
  (zip/xml-zip (xml/parse (fu/absolute-path file)))
  ;;(parse-str (ds/slurp* file))
  )

(def link-reports (fu/re-filter-files #".*linkreport.xml" (fu/ls_r)))
(def processed (doseq [[v] link-reports] (process-report v)))

(def file-str (ds/slurp* (first link-reports)))

(def atom1 (parse-str "<?xml version='1.0' encoding='UTF-8'?>
<feed xmlns='http://www.w3.org/2005/Atom'>
  <id>tag:blogger.com,1999:blog-28403206</id>
  <updated>2008-02-14T08:00:58.567-08:00</updated>
  <title type='text'>n01senet</title>
  <link rel='alternate' type='text/html' href='http://n01senet.blogspot.com/'/>
  <entry>
    <id>1</id>
    <published>2008-02-13</published>
    <title type='text'>clojure is the best lisp yet</title>
    <author><name>Chouser</name></author>
  </entry>
  <entry>
    <id>2</id>
    <published>2008-02-07</published>
    <title type='text'>experimenting with vnc</title>
    <author><name>agriffis</name></author>
  </entry>
</feed>
"))

;;(pp/pprint processed "\n")

;;(def scripts (zfx/xml-> processed (tag= :script)))

;;(pprint scripts)
