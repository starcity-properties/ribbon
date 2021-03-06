(ns ribbon.charge
  (:require [clojure.spec.alpha :as s]
            [ribbon.core :as ribbon]
            [toolbelt.core :as tb]
            [toolbelt.async :as ta]))


;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::description string?)
(s/def ::email string?)
(s/def ::managed-account string?)
(s/def ::customer-id string?)
(s/def ::application-fee pos-int?)


;; =============================================================================
;; Methods
;; =============================================================================


;; ============================================================================
;; Fetch


(defn fetch
  "Fetch the charge identified by `charge-id`."
  [conn charge-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "charges/%s" charge-id)
                         :method     :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :charge-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret ta/chan?)


;; =============================================================================
;; Many


(defn many
  "Retrieve a list of charges."
  [conn & {:keys [customer-id limit source managed-account]
           :or   {limit 10}}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint "charges"
                         :method   :get}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {:limit limit}
                   :customer customer-id
                   :source source)))

(s/def ::limit pos-int?)
(s/def ::object #{"all" "alipay_account" "bank_account" "bitcoin_receiver" "card"})
(s/def ::source (s/or :token string?
                      :map (s/keys :req-un [::object])))
(s/fdef many
        :args (s/cat :conn ribbon/conn?
                     :opts (s/keys* ::opt-un [::managed-account
                                              ::customer-id
                                              ::limit
                                              ::source]))
        :ret ta/chan?)


;; =============================================================================
;; Create


(defn create!
  "Create a new charge."
  [conn amount source & {:keys [application-fee description customer-id email
                                destination managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint "charges"
                         :method   :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {:amount   amount
                    :source   source
                    :currency "usd"}
                   :application_fee application-fee
                   :receipt_email email
                   :customer customer-id
                   :description description
                   :destination destination)))

(s/fdef create!
        :args (s/cat :conn ribbon/conn?
                     :amount integer?
                     :source string?
                     :opts (s/keys* :opt-un [::application-fee
                                             ::description
                                             ::customer-id
                                             :ribbon/managed-account
                                             ::email]))
        :ret ta/chan?)


;; =============================================================================
;; Refund
;; =============================================================================


(defn refund!
  "Refund a charge."
  [conn charge & {:keys [amount metadata reason managed-account]}]
  (ribbon/request conn (toolbelt.core/assoc-when
                        {:endpoint "refunds"
                         :method   :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {:charge charge}
                   :amount (int (* amount 100))
                   :metadata metadata
                   :reason reason)))

(s/def :refund/amount float?)
(s/def ::metadata map?)
(s/def ::reason string?)
(s/fdef refund!
        :args (s/cat :conn ribbon/conn?
                     :charge string?
                     :opts (s/keys* :opt-un [:refund/amount
                                             ::metadata
                                             ::reason
                                             ::managed-account])))


;; =============================================================================
;; repl
;; =============================================================================



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

  (def conn (ribbon/stripe-connection secret-key))

  ;; Works
  (<!! (fetch conn sample-managed-charge {:managed-account sample-managed-account}))

  ;; Works
  (<!! (refund! conn "py_1AawdeJDow24Tc1aNCcUP3ts"
                :managed-account sample-managed-account
                :amount 1900.0))


  (<!! (create! secret-key ))

  )
