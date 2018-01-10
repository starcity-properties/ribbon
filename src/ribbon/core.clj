(ns ribbon.core
  (:require [cheshire.core :as json]
            [clojure.core.async :as a :refer [chan put!]]
            [clojure.spec.alpha :as s]
            [org.httpkit.client :as http]
            [ribbon.codec :as codec]
            [toolbelt.async :as ta]
            [toolbelt.core :as tb]))

;; =============================================================================
;; Interface
;; =============================================================================


(defprotocol RibbonRequest
  "Interface to communicate with the Stripe API."
  (request
    [this conf]
    [this conf params]
    "Make a Stripe API request."))


(defrecord StripeConnection [secret-key])


;; =============================================================================
;; Spec
;; =============================================================================


(s/def :ribbon/managed-account string?)

(s/def ::secret-key string?)
(s/def ::endpoint string?)
(s/def ::method keyword?)
(s/def ::managed-account string?)
(s/def ::config
  (s/keys :req-un [::endpoint ::method]
          :opt-un [::managed-account]))


;; =============================================================================
;; Internal
;; =============================================================================


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


;; =============================================================================
;; Base Request
;; =============================================================================


(def ^:dynamic *managed-account* nil)


(defn- request*
  "Initiate a Stripe API request, producing a `core.async` channel that will
  either have the result of a successful request or exception (in the event of
  an error) `put!` onto it."
  ([secret-key conf]
   (request* secret-key conf {}))
  ([secret-key {:keys [endpoint method managed-account]} params]
   (let [req-map    {:url        (format "%s/%s" base-url endpoint)
                     :method     method
                     :headers    (tb/assoc-when
                                  {"Accept" "application/json"}
                                  "Stripe-Account" (or managed-account
                                                       *managed-account*))
                     :basic-auth [secret-key ""]}
         [k params] (params-for method params)]
     (let [c (chan 1)]
       (http/request (assoc req-map k params) (cb c))
       c))))

(s/fdef request*
        :args (s/cat :secret-key ::secret-key
                     :config ::config
                     :params (s/? map?))
        :ret ta/chan?)


;;; impl
(extend-protocol RibbonRequest
  StripeConnection
  (request
    ([this conf]
     (request* (:secret-key this) conf))
    ([this conf params]
     (request* (:secret-key this) conf params)))

  java.lang.String
  (request
    ([this conf]
     (request* this conf))
    ([this conf params]
     (request* this conf params))))


(defmacro with-connect-account
  "Execute all ribbon requests in the default context of the Stripe connect
  `account-id`."
  [account-id & body]
  `(if (string? ~account-id)
     (with-bindings {#'ribbon.core/*managed-account* ~account-id}
       ~@body)
     (do ~@body)))


;; =============================================================================
;; API
;; =============================================================================


(defn stripe-connection
  "Construct a Stripe connection."
  [secret-key]
  (map->StripeConnection {:secret-key secret-key}))


(defn conn? [x] (satisfies? RibbonRequest x))
