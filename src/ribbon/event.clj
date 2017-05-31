(ns ribbon.event
  (:require [clojure.spec :as s]
            [plumbing.core :as plumbing]
            [ribbon.core :as ribbon]
            [toolbelt.predicates :as p]))

;; =============================================================================
;; Spec
;; =============================================================================

(s/def ::managed-account string?)

;; =============================================================================
;; Fetch
;; =============================================================================

(defn fetch
  "Fetch an event by `event-id`."
  [secret-key event-id & {:as opts}]
  (ribbon/request (merge
                   {:endpoint   (format "events/%s" event-id)
                    :method     :get
                    :secret-key secret-key}
                   opts)))

(s/fdef fetch
        :args (s/cat :secret-key string?
                     :event-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)

(comment
  (do
    (def secret-key "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def event-id "evt_1AN4n4IvRccmW9nOnYTc0zkN"))

  (require '[clojure.core.async :refer [<!!]])

  ;; Works
  (<!! (fetch secret-key event-id))

  )
