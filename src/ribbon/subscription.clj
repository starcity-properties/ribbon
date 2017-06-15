(ns ribbon.subscription
  (:require [clojure.spec :as s]
            [plumbing.core :as plumbing]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]))

;; =============================================================================
;; Specs
;; =============================================================================

(s/def ::managed-account string?)
(s/def ::fee-percent float?)

;; =============================================================================
;; Fetch
;; =============================================================================

(defn fetch
  "Fetch the subscription under `subscription-id`."
  [secret-key subscription-id & {:as opts}]
  (ribbon/request (merge
                   {:endpoint   (format "subscriptions/%s" subscription-id)
                    :method     :get
                    :secret-key secret-key}
                   opts)))

(s/fdef fetch
        :args (s/cat :secret-key string?
                     :subscription-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)

;; =============================================================================
;; Create
;; =============================================================================

(defn create!
  "Create a new subscription for `customer-id` under `plan-id`."
  [secret-key customer-id plan-id & {:keys [source
                                            managed-account
                                            fee-percent
                                            trial-end
                                            quantity]}]
  (when (some? fee-percent)
    (assert managed-account "When a `fee-percent` is specified, a `managed-account` must also be supplied."))
  (ribbon/request (plumbing/assoc-when
                   {:endpoint   "subscriptions"
                    :method     :post
                    :secret-key secret-key}
                   :managed-account managed-account)
                  (plumbing/assoc-when
                   {:customer customer-id
                    :plan     plan-id}
                   :source source
                   :application_fee_percent fee-percent
                   :trial_end trial-end
                   :quantity quantity)))

(s/def ::source string?)
(s/def ::quantity integer?)
(s/def ::trial-end integer?)
(s/fdef create!
        :args (s/cat :secret-key string?
                     :customer-id string?
                     :plan-id string?
                     :opts (s/keys* :opt-un [::source
                                             ::managed-account
                                             ::fee-percent
                                             ::trial-end
                                             ::quantity]))
        :ret p/chan?)

;; =============================================================================
;; Update
;; =============================================================================

(defn update!
  "Update the subscription under `subscription-id`."
  [secret-key subscription-id & {:keys [managed-account fee-percent source quantity]}]
  (when (some? fee-percent)
    (assert managed-account "When a `fee-percent` is specified, a `managed-account` must also be supplied."))
  (ribbon/request (plumbing/assoc-when
                   {:endpoint   (format "subscriptions/%s" subscription-id)
                    :method     :post
                    :secret-key secret-key}
                   :managed-account managed-account)
                  (plumbing/assoc-when
                   {}
                   :source source
                   :application_fee_percent fee-percent
                   :quantity quantity)))

(s/fdef update!
        :args (s/cat :secret-key string?
                     :subscription-id string?
                     :opts (s/keys* :opt-un [::managed-account
                                             ::fee-percent
                                             ::source
                                             ::quantity]))
        :ret p/chan?)

(comment
  (do
    (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def sample-charge "py_1AI03fIvRccmW9nOsOwf13fX")
    (def sample-managed-account "acct_191838JDow24Tc1a")
    (def sample-managed-charge "py_1AEpQcJDow24Tc1a9fCyIdXw")
    (def sample-customer "cus_A7rRh3Ro3c2dmx")
    (def sample-managed-customer "cus_9zbGYwH44Ht3vF")
    (def sample-bank-source "ba_19nbiQIvRccmW9nOqgXy74mB")
    (def sample-managed-bank-source "ba_19fc49JDow24Tc1aH2p5fNG3"))

  (require '[clojure.core.async :refer [<!!]])

  ;; Works
  (<!! (fetch secret-key "sub_9zbG4ycfe4VA1u"
              :managed-account sample-managed-account))

  (<!! (create! secret-key sample-customer "TESTPLAN1"))

  )
