(ns ribbon.core
  (:require [cheshire.core :as json]
            [clojure.core.async :as a :refer [chan put!]]
            [clojure.spec :as s]
            [org.httpkit.client :as http]
            [plumbing.core :as plumbing]
            [ribbon.codec :as codec]
            [toolbelt.predicates :as p]))

(def ^:private base-url
  "https://api.stripe.com/v1")

(defn- params-for
  [method params]
  (case method
    :get [:query-params params]
    [:body (codec/form-encode params)]))

(defn- cb [c]
  (fn [{body :body}]
    (let [body (json/parse-string body true)]
      (if-let [e (:error body)]
        (put! c (ex-info "Error in Stripe request!" e))
        (put! c body))
      (a/close! c))))

(defn request
  "Initiate a Stripe API request, producing a `core.async` channel that will
  either have the result of a successful request or exception (in the event of
  an error) `put!` onto it."
  ([conf]
   (request conf {}))
  ([{:keys [secret-key endpoint method managed-account] :as conf} params]
   (let [req-map    {:url        (format "%s/%s" base-url endpoint)
                     :method     method
                     :headers    (plumbing/assoc-when
                                  {"Accept" "application/json"}
                                  "Stripe-Account" managed-account)
                     :basic-auth [secret-key ""]}
         [k params] (params-for method params)]
     (let [c (chan 1)]
       (http/request (assoc req-map k params) (cb c))
       c))))

(s/def ::secret-key string?)
(s/def ::endpoint string?)
(s/def ::method keyword?)
(s/def ::managed-account string?)
(s/def ::config
  (s/keys :req-un [::secret-key ::endpoint ::method]
          :opt-un [::managed-account]))

(s/fdef request
        :args (s/cat :config ::config :params (s/? map?))
        :ret p/chan?)
