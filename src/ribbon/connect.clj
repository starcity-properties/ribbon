(ns ribbon.connect
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]
            [toolbelt.async :refer [<!!?]]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [toolbelt.core :as tb]))


;; =============================================================================
;; Create Bank Token
;; =============================================================================


(defn create-bank-token!
  "Create a bank token to be attached to a Connect account."
  [conn customer-id bank-token managed-account]
  (ribbon/request conn
                  {:endpoint        "tokens"
                   :method          :post
                   :managed-account managed-account}
                  {:customer     customer-id
                   :bank_account bank-token}))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :customer-id string?
                     :bank-token string?
                     :managed-account string?)
        :ret p/chan?)


;; =============================================================================
;; Create Connected Account
;; =============================================================================


;; =====================================
;; Owner


(defn- dob* [day month year]
  {"legal_entity[dob][day]"   day
   "legal_entity[dob][month]" month
   "legal_entity[dob][year]"  year})


(defn owner
  [first-name last-name dob]
  (let [dob (c/to-date-time dob)]
    (merge {"legal_entity[first_name]" first-name
            "legal_entity[last_name]"   last-name}
           (dob* (t/day dob) (t/month dob) (t/year dob)))))

(s/fdef owner
        :args (s/cat :first-name string?
                     :last-name string?
                     :dob inst?))


;; =====================================
;; Business


(defn address
  [line1 zip & {:keys [line2 city state]
                :or   {city "San Francisco", state "CA"}}]
  (tb/assoc-when
   {"legal_entity[address][city]"        city
    "legal_entity[address][state]"       state
    "legal_entity[address][postal_code]" zip
    "legal_entity[address][line1]"       line1
    "legal_entity[address][country]"     "US"}
   "legal_entity[address][line2]"       line2))


(def default-address
  "Default address that we create building entities under."
  (address "995 Market St." "94103" :line2 "2nd Fl."))


(defn business
  [name tax-id & [address]]
  (merge {"legal_entity[business_name]"   name
          "legal_entity[business_tax_id]" tax-id
          "legal_entity[type]"            "company"}
         (or address default-address)))


;; =====================================
;; Account


(defn account
  [account-number routing-number]
  {"external_account[account_number]" account-number
   "external_account[routing_number]" routing-number
   "external_account[object]"         "bank_account"
   "external_account[country]"        "US"
   "external_account[currency]"       "USD"})


(def tos-ip "45.33.63.187")


(defn create-account!
  "Create a connected account on Stripe."
  [conn owner business account]
  (ribbon/request conn
                  {:endpoint "accounts"
                   :method   :post}
                  (merge
                   {:country               "US"
                    :type                  "custom"
                    "tos_acceptance[ip]"   tos-ip
                    "tos_acceptance[date]" (int (/ (c/to-long (t/now)) 1000))}
                   owner
                   business
                   account)))

(s/fdef create-account!
        :args (s/cat :conn ribbon/conn? :owner map? :business map? :account map?)
        :ret p/chan?)


(comment

  (let [owner    (owner "" "" #inst "2017-01-01")
        business (business "2072-2074 Mission LLC" ""
                           (address "1020 Kearny Street" "94133"))
        account  (account "" "")]
    (<!!? (ribbon/request stripe-key
                          {:endpoint ""
                           :method   :get})))


  )
