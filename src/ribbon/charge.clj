(ns ribbon.charge
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [toolbelt
             [core :as tb]
             [predicates :as p]]))


;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::description string?)
(s/def ::email string?)
(s/def ::managed-account string?)
(s/def ::customer-id string?)


;; =============================================================================
;; Methods
;; =============================================================================


;; =============================================================================
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
        :ret p/chan?)


;; =============================================================================
;; Create


(defn create!
  "Create a new charge."
  [conn amount source & {:keys [description customer-id managed-account email]}]
  (ribbon/request conn {:endpoint   "charges"
                        :method     :post}
                  (tb/assoc-when
                   {:amount   amount
                    :source   source
                    :currency "usd"}
                   :receipt_email email
                   :customer customer-id
                   :description description
                   :destination managed-account)))

(s/fdef create!
        :args (s/cat :conn ribbon/conn?
                     :amount integer?
                     :source string?
                     :opts (s/keys* :opt-un [::description
                                             ::customer-id
                                             ::managed-account
                                             ::email]))
        :ret p/chan?)



;; =============================================================================
;; Refund
;; =============================================================================


(defn refund!
  "Refund a charge."
  [conn charge & {:keys [amount metadata reason managed-account]}]
  (ribbon/request conn (tb/assoc-when
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
  (<!! (create! conn 1000 "card_1ADjO1IvRccmW9nOnXB2upzB"
                {:description "test card charge from Ribbon lib"
                 :customer-id "cus_AYr6e1rdK4hGF6"
                 :email       "josh@joinstarcity.com"}))

  ;; Works
  ;; NOTE: Creates a connect charge via the platform
  (<!! (create! conn 1000 sample-bank-source
                {:managed-account sample-managed-account
                 :description     "test bank charge from Ribbon lib"
                 :customer-id     sample-customer
                 :email           "josh@joinstarcity.com"}))


  (<!! (refund! conn "py_1AawdeJDow24Tc1aNCcUP3ts"
                :managed-account sample-managed-account
                :amount 1900.0))

  )
