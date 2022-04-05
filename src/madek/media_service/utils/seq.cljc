(ns madek.media-service.utils.seq)

(defn with-index [offset xs]
  (map-indexed (fn [idx x]
                 (assoc x :index (+ offset idx)))
               xs))

(defn with-page-index [xs]
  (map-indexed (fn [idx x]
                 (assoc x :page-index idx ))
               xs))

(defn with-key [key-fn xs]
  (map (fn [x]
         (assoc x :key (key-fn x)))
       xs))
