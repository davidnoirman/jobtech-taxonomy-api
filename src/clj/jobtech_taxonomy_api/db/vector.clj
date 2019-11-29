(ns jobtech-taxonomy-api.db.vector
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [jobtech-taxonomy-api.db.concepts :as concepts]
   [jobtech-taxonomy-api.db.api-util :as api-util]
   [clojure.set :as set]
   )
  )


(def s-query
  '[:find

    (pull ?r [{:relation/concept-1 [:concept/id
                                    :concept/type
                                    :concept/preferred-label
                                    ]}
              {:relation/concept-2 [:concept/id
                                    :concept/type
                                    :concept/preferred-label
                                    ]}
              :relation/type
              :relation/substitutability-percentage
              ])

    :in $ ?relation-type
    :where

    [?r :relation/type ?relation-type]

    ])

;; sätt limit 3 på den och få till vector koden


(def relations-query
  '{:find [(pull ?r [{:relation/concept-1 [:concept/id
                                           :concept/type
                                           :concept/preferred-label
                                           ]}
                     {:relation/concept-2 [:concept/id
                                           :concept/type
                                           :concept/preferred-label
                                           ]}
                     :relation/type
                     :relation/substitutability-percentage
                     ])]
    :args [db "substitutability"]
    :in [$ ?relation-type]
    :where [    [?r :relation/type ?relation-type]]
    :limit 1
    :offset 0
    }
  )


;; (def sample (take 10 (d/q relations-query (get-db)  "substitutability")))

(def a-sample [#:relation{:concept-1
                          #:concept{:id "yszw_TRw_S9p",
                                    :type "occupation-name",
                                    :preferred-label "Ammoniakkokare"},
                          :concept-2
                          #:concept{:id "Wrz5_spU_V6Q",
                                    :type "occupation-name",
                                    :preferred-label
                                    "Maskinoperatör, ytbehandling"},
                          :type "substitutability",
                          :substitutability-percentage 25}])

(defn extracs-ids-fun [[{:relation/keys [concept-1 concept-2 type substitutability-percentage]}]]
  [[(:concept/id concept-1) (:concept/id concept-2)]  (* 0.01 substitutability-percentage)]
  )

(defn extracs-label-fun [[{:relation/keys [concept-1 concept-2 type substitutability-percentage]}]]
  [{(:concept/id concept-1) (:concept/preferred-label concept-1)} { (:concept/id concept-2) (:concept/preferred-label concept-2)}]
  )


;; (def matrix (into {} (map extracs-ids-fun sample)))

;; (def ids (sort (set (flatten (keys matrix)))))

;; (def name-lookup (into {} (flatten  (map extracs-label-fun sample))))

(comment
  (defn get-cell [matrix id-1 id-2]
    (if (= id-1 id-2)
      1
      (get matrix [id-1 id-2] 0)
      )
    )

  (defn create-vector [id ids matrix]
    (map #(get-cell matrix id %) ids)
    )

  (defn vector-reducer [acc id]
    (assoc acc id (create-vector id ids matrix))
    ))


#_("yszw_TRw_S9p"
 "666b_sG9_NBS"
 "Ae82_m9Q_4GJ"
 "KWFX_juL_yMb"
 "GZXT_V3B_3yJ"
 "R7Gb_1n3_bwD"
 "WX7N_fPQ_SQz"
 "L5t3_Vf6_qZq"
 "cJ62_FKk_ua6"
 "Wrz5_spU_V6Q"
 "EgyF_T9E_tg6"
 "D9nf_Uo8_zJ4"
 "kkpb_kx9_QKr"
 "Go3i_Vtx_WYN"
 "g5xL_rUr_GoJ"
 "bgTi_SxA_pLX"
 "ezuK_WTk_obQ"
 "924X_QHC_bdK"
 "JYP9_2zs_z1H")

#_((0 0 0 0 0 0 0 0 0 0 0 0 0.25 0 0 0 0 0 1)
 (1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.75 0 0)
 (0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 0.75 0 0 1 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 0 0 0 0 0 1 0 0.25 0 0 0 0 0 0)
 (0 0 0 0 0 0 0.25 0 0 0 0 1 0 0 0 0 0 0 0)
 (0 0 0 0 0.25 0 0 0 0 1 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 0 0 0 0 0 0 0 0 0.75 1 0 0 0 0)
 (0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0)
 (0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0.25 0)
 (0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0)
 (0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0)
 (0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0)
 (0 0 0.25 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0)
 (0 1 0 0 0 0 0 0.25 0 0 0 0 0 0 0 0 0 0 0)
 (0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0))


(comment

#_(map #(get name-lookup %) '("yszw_TRw_S9p"
                                                        "666b_sG9_NBS"
                                                        "Ae82_m9Q_4GJ"
                                                        "KWFX_juL_yMb"
                                                        "GZXT_V3B_3yJ"
                                                        "R7Gb_1n3_bwD"
                                                        "WX7N_fPQ_SQz"
                                                        "L5t3_Vf6_qZq"
                                                        "cJ62_FKk_ua6"
                                                        "Wrz5_spU_V6Q"
                                                        "EgyF_T9E_tg6"
                                                        "D9nf_Uo8_zJ4"
                                                        "kkpb_kx9_QKr"
                                                        "Go3i_Vtx_WYN"
                                                        "g5xL_rUr_GoJ"
                                                        "bgTi_SxA_pLX"
                                                        "ezuK_WTk_obQ"
                                                        "924X_QHC_bdK"
                                                        "JYP9_2zs_z1H"))
#_(
 "Ammoniakkokare"
 "Berglastare"
 "Cementbrännare"
 "Civilingenjör, energi"
 "Energirådgivare"
 "Fogare, faner"
 "Kalandrerare"
 "Kommunsekreterare"
 "Kontrollingenjör, elkraft"
 "Maskinoperatör, ytbehandling"
 "Näringslivssekreterare/Näringslivsutvecklare"
 "Plastsvetsare"
 "Plysare"
 "Pressare, spånskivor"
 "Raslastare"
 "Servicetekniker, elkraft"
 "Slipare, snickeri"
 "Wellpappmaskinförare"
 "Vävare"

   )

  )
