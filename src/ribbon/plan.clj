(ns ribbon.plan
  (:require [clojure.spec :as s]
            [plumbing.core :as plumbing]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]))

(def ^:private max-descriptor-length
  "Max character length of a plan's statement descriptor."
  22)

;; =============================================================================
;; Spec
;; =============================================================================

(s/def ::managed-account string?)

;; =============================================================================
;; Fetch
;; =============================================================================

(defn fetch
  "Fetch the plan identified by `plan-id`."
  [secret-key plan-id & {:as opts}]
  (ribbon/request (merge
                   {:endpoint   (format "plans/%s" plan-id)
                    :method     :get
                    :secret-key secret-key}
                   opts)))

(s/fdef fetch
        :args (s/cat :secret-key string?
                     :plan-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)

;; =============================================================================
;; Create
;; =============================================================================

(defn create!
  "Create a new plan."
  [secret-key plan-id name amount interval & {:keys [trial-days descriptor metadata managed-account]}]
  (when (some? descriptor)
    (assert (<= (count descriptor) max-descriptor-length)
            "The statement descriptor must be less than or equal to 22 characters."))
  (ribbon/request (plumbing/assoc-when
                   {:endpoint   "plans"
                    :method     :post
                    :secret-key secret-key}
                   :managed-account managed-account)
                  (plumbing/assoc-when
                   {:id       plan-id
                    :amount   amount
                    :currency "usd"
                    :interval (clojure.core/name interval)
                    :name     name}
                   :trial_period_days trial-days
                   :statement_descriptor descriptor)))

(s/def ::trial-days pos-int?)
(s/def ::descriptor (s/and string? #(<= (count %) max-descriptor-length)))
(s/def ::metadata map?)
(s/fdef create!
        :args (s/cat :secret-key string?
                     :plan-id string?
                     :name string?
                     :amount pos-int?
                     :interval #{:day :week :month :year}
                     :opts (s/keys* :opt-un [::trial-days
                                             ::descriptor
                                             ::metadata
                                             ::managed-account]))
        :ret p/chan?)

(comment
  (do
    (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def sample-managed-account "acct_191838JDow24Tc1a")
    (def plan-id "285873023222909"))

  (require '[clojure.core.async :refer [<!!]])

  ;; Works
  (<!! (fetch secret-key plan-id
              :managed-account sample-managed-account))

  ;; Works
  (<!! (create! secret-key "TESTPLAN1" "Test Plan 1" 2000 :month
                :trial-days 5
                :descriptor "subs to test plan"))
  )
