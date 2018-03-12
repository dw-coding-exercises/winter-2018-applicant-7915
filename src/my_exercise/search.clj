(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [clojure.string :as string]
            [clojure.edn :as edn]))

(defn header [_]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Find my next election: Results"]
   [:link {:rel "stylesheet" :href "default.css"}]])

; Parse response from API, display list of elections
; Would want to explore displaying this without navigating to /search (a.k.a. SPA), also add error-handling on UI
(defn parse [response]
    [:div {:class "results"}
      [:h1 "Elections"]
      [:ul
        (for [election response]
          (do
            ; Ideally would dynamically show returned fields vs. manually hard-coding them like below
            (def district-divisions (get election :district-divisions))
            (def polling-place-url (get election :polling-place-url-shortened))
            (def description (get election :description))
            (def date (get election :date))
            (def id (get election :id))
            [:li {:election-id id}
              [:h2 description]
              [:p "Date: " date] ; Need to format this date
              [:p
                [:a {:href polling-place-url} "Polling place URL"]]]
          )
        )
      ]
    ]
)

; Error handling for blank city or state
(defn blank-fields [fields]
  [:div {:class "error"}
    [:h1 "ERROR: Blank fields!"]
    [:p "The following fields are required:"]
    [:ul
      (for [blank (filter (fn [field] (string/blank? (val field))) fields)]
        [:li (key blank)])]
  ])

; Get inputted form POST data
; Only looking for city and state fields currently, would use other fields to get more data if possible
(defn getdata [request]
    (def params (get request :form-params))
    (def state (str (get params "state")))
    (def city (str (get params "city")))
    (if (or (string/blank? state) (string/blank? city))
        (do (blank-fields {"city" city "state" state}))
        (do
            (def stateocd (str "ocd-division/country:us/state:" (.toLowerCase state)))
            (def cityocd (str stateocd "/place:" (string/replace city " " "_")))
            (def ocds (str stateocd "," cityocd))
            (def elections-string (edn/read-string (slurp (str "https://api.turbovote.org/elections/upcoming?district-divisions=" ocds))))
            (parse elections-string)
        )
    )
)

(defn results [request]
   (getdata request))

(defn page [request]
  (html5
   (header request)
   (results request)))
