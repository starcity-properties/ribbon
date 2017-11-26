(ns ribbon.util
  (:require [toolbelt.core :as tb]
            [clojure.spec :as s]
            [clj-time.coerce :as c]))


;; =============================================================================
;; Date Params
;; =============================================================================


(s/def ::gt inst?)
(s/def ::gte inst?)
(s/def ::lt inst?)
(s/def ::lte inst?)
(s/def ::date-params
  (s/keys :opt-un [::gt ::gte ::lt ::lte]))


(defn date-params? [x]
  (s/valid? ::date-params x))


(defn format-date-params [params]
  (when-some [params params]
    (tb/transform-when-key-exists params
      {:gt  c/to-epoch
       :gte c/to-epoch
       :lt  c/to-epoch
       :lte c/to-epoch})))


;; =============================================================================
;; Managed Accounts
;; =============================================================================


(defn inject-managed [m opts]
  (tb/assoc-when m :managed-account (:managed-account opts)))
