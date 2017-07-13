(ns ribbon.connect
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]))


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
