(ns ribbon.plan
  (:require [clojure.spec.alpha :as s]
            [ribbon.core :as ribbon]
            [toolbelt.async :as ta]
            [toolbelt.core :as tb]))


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
  [conn plan-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "plans/%s" plan-id)
                         :method     :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :plan-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret ta/chan?)


;; =============================================================================
;; Create
;; =============================================================================


(defn create!
  "Create a new plan."
  [conn plan-id name amount interval & {:keys [trial-days descriptor metadata managed-account]}]
  (when (some? descriptor)
    (assert (<= (count descriptor) max-descriptor-length)
            "The statement descriptor must be less than or equal to 22 characters."))
  (ribbon/request conn (tb/assoc-when
                        {:endpoint   "plans"
                         :method     :post}
                        :managed-account managed-account)
                  (tb/assoc-when
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
        :args (s/cat :conn ribbon/conn?
                     :plan-id string?
                     :name string?
                     :amount pos-int?
                     :interval #{:day :week :month :year}
                     :opts (s/keys* :opt-un [::trial-days
                                             ::descriptor
                                             ::metadata
                                             ::managed-account]))
        :ret ta/chan?)


;; ==============================================================================
;; delete -----------------------------------------------------------------------
;; ==============================================================================


(defn delete!
  "Delete a plan."
  [conn plan-id & {:keys [managed-account]}]
  (ribbon/request conn (tb/assoc-when
                        {:endpoint (format "plans/%s" plan-id)
                         :method   :delete}
                        :managed-account managed-account)))

(s/fdef delete!
        :args (s/cat :conn ribbon/conn?
                     :plan-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret ta/chan?)


;; =============================================================================
;; repl
;; =============================================================================


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
