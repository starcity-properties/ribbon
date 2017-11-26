(ns ribbon.balance
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [ribbon.util :as util]
            [toolbelt.predicates :as p]
            [toolbelt.core :as tb]))

;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::available-on util/date-params?)
(s/def ::created util/date-params?)
(s/def ::currency string?)
(s/def ::ending-before string?)
(s/def ::limit #(and (> % 0) (<= % 100)))
(s/def ::payout string?)
(s/def ::source string?)
(s/def ::starting-after string?)
(s/def ::type
  #{"charge" "refund" "adjustment" "application_fee" "application_fee_refund"
    "transfer" "payment" "payout" "payout_failure" "stripe_fee"})


;; =============================================================================
;; Retrieve
;; =============================================================================


(defn retrieve
  "Retrieves the current account balance."
  [conn & {:as opts}]
  (ribbon/request conn (util/inject-managed {:endpoint "balance"
                                             :method   :get} opts)))

(s/fdef retrieve
        :args (s/cat :conn ribbon/conn?
                     :opts (s/keys* :opt-un [:ribbon/managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Retrieve Transaction
;; =============================================================================


(defn retrieve-transaction
  "Retrieves the balance transaction with the given ID."
  [conn transaction-id & {:as opts}]
  (ribbon/request conn (util/inject-managed
                        {:endpoint (format "balance/history/%s" transaction-id)
                         :method   :get} opts)))

(s/fdef retrieve-transaction
        :args (s/cat :conn ribbon/conn?
                     :transaction-id string?
                     :opts (s/keys* :opt-un [:ribbon/managed-account])))


;; =============================================================================
;; List All
;; =============================================================================


(defn list-all
  "Returns a list of transactions that have contributed to the Stripe account
  balance, (e.g., charges, transfers and so forth)."
  [conn & {:keys [available-on created currency ending-before limit payout
                  source starting-after type]
           :as   opts}]
  (ribbon/request conn (util/inject-managed
                        {:endpoint "balance/history"
                         :method   :get} opts)
                  (tb/assoc-when
                   {}
                   :available_on (util/format-date-params available-on)
                   :created (util/format-date-params created)
                   :currency currency
                   :ending_before ending-before
                   :limit limit
                   :payout payout
                   :source source
                   :starting_after starting-after
                   :type type)))

(s/fdef list-all
        :args (s/cat :conn ribbon/conn?
                     :opts (s/keys* :opt-un [::available-on
                                             ::created
                                             ::currency
                                             ::ending-before
                                             ::limit
                                             ::payout
                                             ::source
                                             ::starting_after
                                             ::type
                                             :ribbon/managed-account]))
        :ret p/chan?)


(comment

  (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (clojure.core.async/<!! (retrieve secret-key))

  (clojure.core.async/<!! (list-all secret-key :type "application_fee"))

  (clojure.core.async/<!! (retrieve-transaction secret-key "txn_1BJV6YIvRccmW9nOy53FIab8"))

  )
