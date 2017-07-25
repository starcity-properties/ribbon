(ns ribbon.customer
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [toolbelt
             [core :as tb]
             [predicates :as p]]))


;; =============================================================================
;; Interface
;; =============================================================================


#_(defprotocol StripeCustomer
    "Interface for interacting with a Stripe customer."
    (customer-id [this]
      "Produce the customer's ID.")
    (sources [this]
      "Produce the customer's list of sources.")
    (default-source [this]
      "Produce the customer's default source.")
    (bank-accounts [this]
      "Produce the customer's bank accounts.")
    (active-bank-account [this]
      "The active bank account.")
    (has-bank-account? [this]
      "Does customer have a bank account?")
    (has-verified-bank-account? [this]
      "Does customer have a verified bank account?")
    (verification-failed? [this]
      "Did the customer's verification attempt fail?")
    (credit-cards [this]
      "Produce the customer's credit cards")
    (active-credit-card [this]
      "The active credit card."))


;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::managed-account string?)
(s/def ::description string?)
(s/def ::customer (s/and map? #(= "customer" (:object %))))
(s/def ::source (s/and map? (comp #{"bank_account" "card"} :object)))
(s/def ::bank-account (s/and map? #(= "bank_account" (:object %))))
(s/def ::card (s/and map? #(= "card" (:object %))))


;; =============================================================================
;; Predicates
;; =============================================================================


(defn customer?
  "Is `m` a customer response from Stripe?"
  [m]
  (= "customer" (:object m)))

(s/fdef customer?
        :args (s/cat :customer ::customer)
        :ret boolean?)


(def source?
  "Is `m` a source?"
  (comp boolean #{"bank_account" "card"} :object))

(s/fdef source?
        :args (s/cat :customer ::customer)
        :ret boolean?)


;; =============================================================================
;; Sources
;; =============================================================================


(def token
  "The `source`'s token."
  :id)

(s/fdef token
        :args (s/cat :source ::source)
        :ret string?)


(def account-name
  "The bank account's name."
  :bank_name)

(s/fdef account-name
        :args (s/cat :bank-account ::bank-account)
        :ret string?)


(def account-last4
  "The last four digits of the bank account number."
  :last4)

(s/fdef account-last4
        :args (s/cat :bank-account ::bank-account)
        :ret string?)


(defn verified-bank-account?
  "Is this bank account verified?"
  [bank-account]
  (= "verified" (:status bank-account)))

(s/fdef verified-bank-account?
        :args (s/cat :bank-account ::source)
        :ret boolean?)


(defn unverified-bank-account?
  "Is this bank account unverified?"
  [bank-account]
  (not (#{"verified" "verification_failed"} (:status bank-account))))

(s/fdef unverified-bank-account?
        :args (s/cat :bank-account ::source)
        :ret boolean?)


;; =============================================================================
;; Customer
;; =============================================================================


(def customer-id
  "The `customer`'s id."
  :id)

(s/fdef customer-id
        :args (s/cat :customer ::customer)
        :ret string?)


(defn sources
  "The `customer`'s payment sources"
  [customer]
  (get-in customer [:sources :data]))

(s/fdef sources
        :args (s/cat :customer ::customer)
        :ret (s/* map?))


(defn default-source
  "The `customer`'s default payment source."
  [customer]
  (:default_source customer))

(s/fdef default-source
        :args (s/cat :customer ::customer)
        :ret string?)


(defn default-source-type
  "Produce the type of the `customer`"
  [customer]
  (let [token (:default_source customer)]
    (:object
     (tb/find-by
      #(= token (:id %))
      (sources customer)))))

(s/fdef default-source-type
        :args (s/cat :customer ::customer)
        :ret (s/or :nothing nil? :type #{"bank_account" "card"}))


(defn bank-accounts
  "The customer's bank account sources."
  [customer]
  (filter #(= "bank_account" (:object %)) (sources customer)))

(s/fdef bank-accounts
        :args (s/cat :customer ::customer)
        :ret (s/* ::bank-account))


(defn unverified-bank-account
  "The customer's bank account that is pending verification."
  [customer]
  (tb/find-by unverified-bank-account? (bank-accounts customer)))

(s/fdef unverified-bank-account
        :args (s/cat :customer ::customer)
        :ret (s/or :bank-account ::bank-account :nothing nil?))


(defn active-bank-account
  "The customer's active bank account."
  [customer]
  (let [default-source (:default_source customer)
        bank-accounts  (bank-accounts customer)
        default-bank   (tb/find-by (comp #{default-source} :id) bank-accounts)]
    (or default-bank (first bank-accounts))))

(s/fdef active-bank-account
        :args (s/cat :customer ::customer)
        :ret (s/or :bank-account ::bank-account :nothing nil?))


(def has-bank-account?
  "Does `customer` have a bank account linked?"
  (comp boolean active-bank-account))

(s/fdef has-bank-account?
        :args (s/cat :customer ::customer)
        :ret boolean?)


(defn has-verified-bank-account?
  "Does `customer` have a verified bank account?"
  [customer]
  (boolean (tb/find-by verified-bank-account? (bank-accounts customer))))

(s/fdef has-verified-bank-account?
        :args (s/cat :customer ::customer)
        :ret boolean?)


(defn verification-failed?
  "Has `customer`'s verification attempt(s) failed?"
  [customer]
  (let [accounts (bank-accounts customer)]
    (and (not (empty? accounts))
         (every?
          (fn [{status :status}] (= status "verification_failed"))
          accounts))))

(s/fdef verification-failed?
        :args (s/cat :customer ::customer)
        :ret boolean?)


(defn credit-cards
  "The customer's credit card sources."
  [customer]
  (filter #(= "card" (:object %)) (sources customer)))

(s/fdef credit-cards
        :args (s/cat :customer ::customer)
        :ret (s/* ::card))


(defn active-credit-card
  "The customer's active credit card."
  [customer]
  (let [default (default-source customer)]
    (if (= "card" (:object default))
      default
      (first (credit-cards customer)))))

(s/fdef active-credit-card
        :args (s/cat :customer ::customer)
        :ret (s/or :card ::card :nothing nil?))


;; =============================================================================
;; Fetch
;; =============================================================================


(defn fetch
  "Fetch the customer identified by `customer-id`."
  [conn customer-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "customers/%s" customer-id)
                         :method     :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Create
;; =============================================================================


(defn create!
  "Create a new Stripe customer."
  [conn email source & {:keys [description managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint   "customers"
                         :method     :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {:email  email
                    :source source}
                   :description description)))

(s/fdef create!
        :args (s/cat :conn ribbon/conn?
                     :email string?
                     :source string?
                     :opts (s/keys* :opt-un [::managed-account ::description]))
        :ret p/chan?)


;; =============================================================================
;; Update
;; =============================================================================


(defn update!
  "Update an existing Stripe customer."
  [conn customer-id & {:keys [managed-account default-source description]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint   (format "customers/%s" customer-id)
                         :method     :post}
                        :managed-account managed-account)
                  (tb/assoc-when
                   {}
                   :description description
                   :default_source default-source)))

(s/def ::default-source string?)
(s/fdef update!
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :opts (s/keys* :opt-un [::managed-account ::description ::default-source]))
        :ret p/chan?)


;; =============================================================================
;; Delete
;; =============================================================================


(defn delete!
  "Delete the stripe customer identified by `customer-id`."
  [conn customer-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "customers/%s" customer-id)
                         :method     :delete}
                        opts)))

(s/fdef delete!
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :opts (s/keys :opt-un [::managed-account]))
        :ret p/chan?)


;; =============================================================================
;; Verify Bank Account
;; =============================================================================


(defn verify-bank-account!
  "Verify the microdeposits for `customer-id`."
  [conn customer-id bank-token amount-1 amount-2 & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "customers/%s/sources/%s/verify"
                                             customer-id bank-token)
                         :method     :post}
                        opts)
                  {:amounts [amount-1 amount-2]}))

(s/def ::deposit-amount (s/and integer? #(> % 0) #(< % 100)))
(s/fdef verify-bank-account!
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :bank-token string?
                     :amount-1 ::deposit-amount
                     :amount-2 ::deposit-amount
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)

;; =============================================================================
;; Add Source
;; =============================================================================


(defn add-source!
  "Add `source` to the customer identified by `customer-id`."
  [conn customer-id source & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "customers/%s/sources" customer-id)
                         :method     :post}
                        opts)
                  {:source source}))

(s/fdef add-source!
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :source string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)


(defn delete-source!
  "Delete the source identified by `source-id` from the customer identified by
  `customer-id`."
  [conn customer-id source-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint (format "customers/%s/sources/%s" customer-id source-id)
                         :method   :delete}
                        opts)))

(s/fdef delete-source!
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :source-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)


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

  ;; Works
  (<!! (fetch secret-key sample-managed-customer
              :managed-account sample-managed-account))

  ;; Works
  (<!! (fetch secret-key sample-customer))

  ;; Works
  (<!! (create! secret-key "jalehman37@gmail.com" sample-bank-source))

  ;; Works
  (<!! (delete! secret-key "cus_AbL2DNSH0T7Hl2"))

  ;; Works
  (<!! (delete! secret-key "cus_9v4UJ37PFbI4J6"
                :managed-account sample-managed-account))


  )
