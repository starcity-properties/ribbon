(ns ribbon.source
  (:require [clojure.spec :as s]
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
  "Fetch the source identified by `source-id`."
  [conn source-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint (format "sources/%s" source-id)
                         :method   :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :source-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret p/chan?)
