(ns ribbon.event
  (:require [clojure.spec :as s]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]))


;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::managed-account string?)


;; =============================================================================
;; Selectors
;; =============================================================================


(defn id [event]
  (:id event))

(s/fdef id
        :args (s/cat :event map?)
        :ret string?)


(defn subject [event]
  (get-in event [:data :object]))

(s/fdef subject
        :args (s/cat :event map?)
        :ret map?)


(defn subject-id [event]
  (get-in event [:data :object :id]))

(s/fdef subject-id
        :args (s/cat :event map?)
        :ret string?)


;; =============================================================================
;; Methods
;; =============================================================================


(defn fetch
  "Fetch an event by `event-id`, optionally specifying the `managed-account`
  from which to fetch it.."
  [conn event-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "events/%s" event-id)
                         :method     :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :event-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)


;; =============================================================================
;; repl
;; =============================================================================


(comment
  (do
    (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def event-id "evt_1AYJMHIvRccmW9nO5FPxwRwc"))

  (require '[clojure.core.async :refer [<!! put! chan]])

  (let [conn (ribbon/stripe-connection secret-key)]
    (<!! (fetch conn event-id)))


  ;; Works
  (<!! (fetch secret-key event-id))

  )
