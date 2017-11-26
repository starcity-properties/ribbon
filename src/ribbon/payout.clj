(ns ribbon.payout
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [ribbon.util :as util]
            [toolbelt.core :as tb]
            [toolbelt.predicates :as p]))

;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::currency string?)
(s/def ::description string?)
(s/def ::destination string?)
(s/def ::metadata map?)
(s/def ::method #{"instant" "standard"})
(s/def ::source-type #{"alipay_account" "bank_account" "card"})
(s/def ::statement-descriptor (s/and string? #(<= (count %) 22)))

(s/def ::arrival-date util/date-params?)
(s/def ::created util/date-params?)
(s/def ::ending-before string?)
(s/def ::limit #(and (> % 0) (<= % 100)))
(s/def ::starting-after string?)
(s/def ::status #{"pending" "paid" "failed" "canceled"})


;; =============================================================================
;; Create
;; =============================================================================


(defn create!
  "Create a new payout."
  [conn amount & {:keys [currency description destination metadata method
                         source-type statement-descriptor managed-account]
                  :or   {currency "USD", method "standard"}}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint "payouts"
                         :method   :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {:amount   amount
                    :currency currency}
                   :description description
                   :destination destination
                   :metadata metadata
                   :method method
                   :source_type source-type
                   :statement_descriptor statement-descriptor)))

(s/fdef create!
        :args (s/cat :conn ribbon/conn?
                     :amount pos-int?
                     :opts (s/keys* :opt-un [::currency
                                             ::description
                                             ::destination
                                             ::metadata
                                             ::method
                                             ::source-type
                                             :ribbon/managed-account
                                             ::statement-descriptor]))
        :ret p/chan?)


;; =============================================================================
;; Fetch
;; =============================================================================


(defn fetch
  "Fetch a payout by `payout-id`."
  [conn payout-id & {:keys [managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint (format "payouts/%s" payout-id)
                         :method   :get}
                        :managed-account managed-account)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :payout-id string?
                     :opts (s/keys* :opt-un [:ribbon/managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Update
;; =============================================================================


(defn update!
  "Update the payout with `payout-id`."
  [conn payout-id & {:keys [metadata managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint (format "payouts/%s" payout-id)
                         :method   :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {}
                   :metadata metadata)))

(s/fdef update!
        :args (s/cat :conn ribbon/conn?
                     :payout-id string?
                     :opts (s/keys* :opt-un [::metadata
                                             :ribbon/managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Many
;; =============================================================================


(defn many
  "Query many payouts with opts."
  [conn & {:keys [arrival-date created destination ending-before limit
                  starting-after status managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint "payouts"
                         :method   :get}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {}
                   :arrival_date (util/format-date-params arrival-date)
                   :created (util/format-date-params created)
                   :destination destination
                   :ending_before ending-before
                   :limit limit
                   :starting_after starting-after
                   :status status)))

(s/fdef many
        :args (s/cat :conn ribbon/conn?
                     :opts (s/keys* :opt-un [::arrival-date
                                             ::created
                                             ::destination
                                             ::ending-before
                                             ::limit
                                             ::starting-after
                                             ::status
                                             :ribbon/managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Cancel
;; =============================================================================


(defn cancel!
  "Cancel payout with `payout-id`."
  [conn payout-id & {:keys [managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint (format "payouts/%s/cancel" payout-id)
                         :method   :post}
                        :managed-account managed-account)))

(s/fdef cancel!
        :args (s/cat :conn ribbon/conn?
                     :payout-id string?
                     :opts (s/keys* :opt-un [:ribbon/managed-account]))
        :ret p/chan?)



(comment

  (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")


  (clojure.core.async/<!! (fetch secret-key "po_1BQQT1IvRccmW9nOO6g4ixLd"))

  (clojure.core.async/<!! (fetch secret-key "po_1BNWpOJDow24Tc1abzze9DnX"
                                 :managed-account "acct_191838JDow24Tc1a"))

  (clojure.core.async/<!! (many secret-key :created {:gt (c/to-date (t/minus (t/now) (t/weeks 1)))}))


  )
