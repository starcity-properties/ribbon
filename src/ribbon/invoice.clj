(ns ribbon.invoice
  (:require [clojure.spec.alpha :as s]
            [ribbon.core :as ribbon]
            [toolbelt.async :as ta]))


;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::managed-account string?)


;; =============================================================================
;; Methods
;; =============================================================================


;; =============================================================================
;; Fetch


(defn fetch
  "Fetch the invoice identified by `invoice-id`."
  [conn invoice-id & {:as opts}]
  (ribbon/request conn (merge
                        {:endpoint   (format "invoices/%s" invoice-id)
                         :method     :get}
                        opts)))

(s/fdef fetch
        :args (s/cat :conn ribbon/conn?
                     :invoice-id string?
                     :opts (s/keys* :opt-un [::managed-account]))
        :ret ta/chan?)
