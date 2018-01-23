(defproject starcity/ribbon "0.10.0"
  :description "core.async based interface to Stripe REST API."
  :url "https://github.com/starcity-properties/ribbon"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [http-kit "2.2.0"]
                 [cheshire "5.8.0"]
                 [clj-time "0.14.2"]
                 [starcity/toolbelt-async "0.4.0"]
                 [starcity/toolbelt-core "0.3.0"]]

  :plugins [[s3-wagon-private "1.2.0"]]

  :repositories {"releases" {:url        "s3://starjars/releases"
                             :username   :env/aws_access_key
                             :passphrase :env/aws_secret_key}})
