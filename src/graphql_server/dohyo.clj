(ns graphql-server.dohyo
  (:require [clojure.core.async :refer [>!!]]
            [integrant.core :as ig]
            [graphql-server.boundary.db :as db]))

(defmethod ig/init-key :graphql-server/dohyo [_ {:keys [db channel]}]
  (fn []
    (let [rikishis (db/find-rikishis db nil nil nil nil)
          higashi (:node (rand-nth (:edges rikishis)))
          nishi (loop [rikishi (:node (rand-nth (:edges rikishis)))]
                  (if (not= (:sumobeya rikishi) (:sumobeya higashi))
                    rikishi
                    (recur (:node (rand-nth (:edges rikishis))))))
          torikumi (db/create-torikumi db
                                       {:higashi higashi
                                        :nishi nishi
                                        :shiroboshi (if (rand-nth [true false])
                                                      higashi nishi)
                                        :kimarite (rand-nth ["TSUKIDASHI" "TSUKITAOSHI" "OSHIDASHI" "OSHITAOSHI" "YORIKIRI" "YORITAOSHI" "ABISETAOSHI" "UWATENAGE" "SHITATENAGE" "KOTENAGE"
                                                             "SUKUINAGE" "UWATEDASHINAGE" "SHITATEDASHINAGE" "KOSHINAGE" "KUBINAGE" "IPPONZEOI" "NICHONAGE" "YAGURANAGE" "KAKENAGE"
                                                             "TSUKAMINAGE" "UCHIGAKE" "SOTOGAKE" "CHONGAKE" "KIRIKAESHI" "KAWAZUGAKE" "KEKAESHI" "KETAGURI" "MITOKOROZEME" "WATASHIKOMI"
                                                             "NIMAIGERI" "KOMATASUKUI" "SOTOKOMATA" "OMATA" "TSUMATORI" "KOZUMATORI" "ASHITORI" "SUSOTORI" "SUSOHARAI" "IZORI" "SHUMOKUZORI"
                                                             "KAKEZORI" "TASUKIZORI" "SOTOTASUKIZORI" "TSUTAEZORI" "TSUKIOTOSHI" "MAKIOTOSHI" "TOTTARI" "SAKATOTTARI" "KATASUKASHI"
                                                             "SOTOMUSO" "UCHIMUSO" "ZUBUNERI" "UWATEHINERI" "SHITATEHINERI" "AMIUCHI" "SABAORI" "HARIMANAGE" "OSAKATE" "KAINAHINERI"
                                                             "GASSHOHINERI" "TOKKURINAGE" "KUBIHINERI" "KOTEHINERI" "HIKIOTOSHI" "HIKKAKE" "HATAKIKOMI" "SOKUBIOTOSHI" "TSURIDASHI"
                                                             "OKURITSURIDASHI" "TSURIOTOSHI" "OKURITSURIOTOSHI" "OKURIDASHI" "OKURITAOSHI" "OKURINAGE" "OKURIGAKE" "OKURIHIKIOTOSHI"
                                                             "WARIDASHI" "UTCHARI" "KIMEDASHI" "KIMETAOSHI" "USHIROMOTARE" "YOBIMODOSHI" "ISAMIASHI" "KOSHIKUDAKE" "TSUKITE" "TSUKIHIZA"
                                                             "FUMIDASHI"])})]
      (>!! (:channel channel)
           {:msg-type :torikumi/updated
            :data {:msg "Updated!" :torikumi torikumi}}))))
