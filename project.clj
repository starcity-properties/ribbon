(defproject starcity/ribbon "0.8.0"
  :description "core.async based interface to Stripe REST API."
  :url "https://github.com/starcity-properties/ribbon"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/core.async "0.2.395"]
                 [http-kit "2.2.0"]
                 [cheshire "5.6.3"]
                 [starcity/toolbelt "0.1.7"]]

  :plugins [[s3-wagon-private "1.2.0"]]

  :repositories {"releases" {:url        "s3://starjars/releases"
                             :username   :env/aws_access_key
                             :passphrase :env/aws_secret_key}})
