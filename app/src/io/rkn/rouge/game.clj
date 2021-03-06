(ns ^:shared io.rkn.rouge.game
  (:require [io.pedestal.app.messages :as msg]
            [io.rkn.rouge.game.board :as b]
            [io.rkn.rouge.game.pieces :as p]
            [io.rkn.util.platform :as plat]
            [io.rkn.rouge.game.timers :as t]))

(defn game-over?
  "Is a game over? (i.e. piece is in an invalid position)"
  ([_ board] (game-over? board))
  ([board]
   (not (b/valid-posn? board))))

;; Transforms
(defn new-game [_ msg]
  (let [rows (get msg :rows 20)
        cols (get msg :cols 10)]
    {:board {:grid (b/empty-grid rows cols)
             :piece nil
             :score 0
             :lines-cleared 0}}))

(defn refresh-piece [board _]
  (let [next-piece (or (:next-piece board) (p/random-tetra))]
    (-> board
        (assoc-in [:piece] next-piece)
        (assoc-in [:next-piece] (p/random-tetra)))))

(defn land-piece
  ([board {:keys [timeout]}]
   (if (= timeout (:landing-channel board))
     (land-piece board)
     board))
  ([board]
   (-> board
       (assoc :grid (b/graft-piece-to-grid board))
       (dissoc :piece))))

(defn lower-piece
  ([board {:keys [timeout]}]
   (if (= timeout (:gravity-channel board))
     (lower-piece board)
     board))
  ([board]
   (if (:piece board)
     (update-in board [:piece :position :row] inc))))

(defn level
  "Return the board's level.

  = Quotient( cleared-lines / 10 ) + 1"
  ([_ board] (level board))
  ([board] (+ 1 (quot (get board :lines-cleared 0) 10))))

(def points {1 100
             2 300
             3 500
             4 800})

(defn bump-score
  "Increase the score of a board by cleared-points * level."
  [board cleared]
  (let [to-add (* (get points cleared 0)
                  (level board))]
    (update-in board [:score] #(+ % to-add))))

(defn clear-lines [board _]
  (let [cleared-lines (count (filter b/full-row? (:grid  board)))
        cleared-board (b/clear-lines board)]
    (-> cleared-board
        (bump-score cleared-lines)
        (update-in [:lines-cleared] #(+ % cleared-lines)))))

(defn player-input [board {:keys [direction]}]
  (let [potential (condp = direction
                    :left (update-in board [:piece :position :col] dec)
                    :right (update-in board [:piece :position :col] inc)
                    :up (p/rotate board)
                    :down (if (:about-to-collide? board)
                            (land-piece board)
                            (lower-piece board))
                    board)]
    (if (b/valid-posn? potential)
      potential
      board)))


;; Continues
(defn refresh-piece-if-missing [piece]
  (when-not piece
    [{msg/type :refresh-piece msg/topic [:game :board]}]))

(defn clear-lines-if-clearable [grid]
  (when (b/clearable? grid)
    [{msg/type :clear-lines msg/topic [:game :board]}]))

;; Derives

(defn next-piece-display [_ board]
  (let [mini-board (b/empty-grid 4 12)
        piece (-> (:next-piece board)
                  (assoc :position {:row 0 :col 0}))]
    (b/graft-piece-to-grid {:grid mini-board
                            :piece piece})))

(defn about-to-collide?
  "Will a piece collide with the board's grid after the next step of gravity?"
  ([_ board] (about-to-collide? board))
  ([board] (-> board
               lower-piece
               b/collided?)))

(defn start-gravity-countdown [_ {:keys [game-over? landing?]}]
  (when-not (or game-over? landing?)
    (t/timeout 1000)))

(defn start-landing-countdown [_ {:keys [game-over? about-to-collide?]}]
  (when (and (not game-over?)
             about-to-collide?)
    (t/timeout 500)))

;; Effects
(defn affect-gravity [timeout-ch]
  (when timeout-ch
    [{msg/type :lower-piece msg/topic [:game :board] :timeout timeout-ch}]))

(defn affect-landing [timeout-ch]
  (when timeout-ch
    [{msg/type :land-piece msg/topic [:game :board] :timeout timeout-ch}]))
