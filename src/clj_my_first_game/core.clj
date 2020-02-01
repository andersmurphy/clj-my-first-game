(ns clj-my-first-game.core
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.paint Color]
           [javafx.scene.control DialogEvent Dialog ButtonType ButtonBar$ButtonData]))

(def tile-size 50)
(def obstacles {:pit {:name "Deep Pit"
                      :image "DeepPitImage.png"
                      :solved-by-tools #{:jumping-legs}}})
(def tools {:jumping-legs {:name "Jumping Legs"
                           :image "JumpingLegs.png"
                           :loses-memory :bicycle}})
(def memories {:bicycle {:name "Bicycle"
                         :image "BicycleImage.png"}})

(def *state
  (atom {:entities [{:color  Color/GREEN
                     :pos    {:x 1 :y 1}
                     :height tile-size
                     :width  tile-size}
                    {:color  Color/GREEN
                     :pos    {:x 2 :y 2}
                     :height tile-size
                     :width  tile-size}
                    {:color  Color/GREEN
                     :pos    {:x 3 :y 3}
                     :height tile-size
                     :width  tile-size}]
         :current-tools []
         :current-memories [:bicycle]
         :current-obstacle nil
         }
        )
  )

(defn choice-dialog
  "Show the obstacle the player has collided with"
  []
  (println "ShowObstacle")
  (println (obstacles (*state :current-obstacle)))
  {:fx/type :choice-dialog
   :showing true
   :on-close-request (fn [^DialogEvent e]
                       (when (nil? (.getResult ^Dialog (.getSource e)))
                         (.consume e)))
   :on-hidden (fn [_]
                (swap! *state assoc-in [:current-obstacle] nil))
   :header-text "Please choose a memory to discard"
   :items [{:id :memory1}
           {:id :memory2}
           {:id :memory3}]})

(defn draw-entity [^Canvas canvas {color       :color
                                   {x :x y :y} :pos}]
  (doto (.getGraphicsContext2D canvas)
    (.setFill color)
    (.fillRect (* tile-size x) (* tile-size y) tile-size tile-size)))

(defn draw-entities [canvas-width canvas-height entities ^Canvas canvas]
  (doto (.getGraphicsContext2D canvas)
    (.clearRect 0 0 canvas-width canvas-height))
  (run! (partial draw-entity canvas) entities))

(defn root-view [{{:keys [entities]} :state}]
  (let [canvas-width  400
        canvas-height 400]
    {:fx/type :stage
     :showing true
     :width   canvas-width
     :height  canvas-height
     :x       200
     :y       200
     :scene   {:fx/type        :scene
               :root           {:fx/type  :h-box
                                :children [{:fx/type :canvas
                                            :height  canvas-height
                                            :width   canvas-width
                                            :draw    (partial draw-entities
                                                              canvas-width
                                                              canvas-height
                                                              entities)}]}
               :on-key-pressed {:event/type :event/scene-key-press}}}))

(defmulti event-handler :event/type)

(def key->action
  {"W" (fn [_] (swap! *state update-in [:entities 0 :pos :y] dec))
   "S" (fn [_] (swap! *state update-in [:entities 0 :pos :y] inc))
   "A" (fn [_] (swap! *state update-in [:entities 0 :pos :x] dec))
   "D" (fn [_] (swap! *state update-in [:entities 0 :pos :x] inc))
   "X" (fn [_] (swap! *state update-in [:current-obstacle] :pit))
   "Y" (fn [_] (swap! *state update-in [:current-obstacle] nil))
   })

(defmethod event-handler :event/scene-key-press [e]
  (let [key-code (str (.getCode ^KeyEvent (:fx/event e)))
        action   (key->action key-code )]
    (when action (action e))))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type fx/ext-many
                                    :desc (if (state :current-obstacle)
                                            [{:fx/type root-view}, {:fx/type choice-dialog}]
                                            [{:fx/type root-view}])}))
   :opts {:fx.opt/map-event-handler event-handler}))

(comment (fx/mount-renderer *state renderer))
