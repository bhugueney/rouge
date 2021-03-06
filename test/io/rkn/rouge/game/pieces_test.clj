(ns io.rkn.rouge.game.pieces-test
  (:require [clojure.test :refer :all]
            [io.rkn.rouge.game.pieces :refer :all]))

#_(deftest max-kick-test
  (is (= 0 (max-kick (tetras :square))))
  (is (= 1 (max-kick (tetras :t))))
  (is (= 3 (max-kick (tetras :line)))))

#_(deftest next-rotation-shape-test
  (let [one-rotation-piece {:shape :a
                            :rotations []}]
    (is (= one-rotation-piece (next-rotation-shape one-rotation-piece))))
  (let [two-rotation-piece {:shape :a
                            :rotations [:b]}]
    (is (= {:shape :b
            :rotations [:a]}
           (next-rotation-shape two-rotation-piece)))
    (is (= two-rotation-piece
           (-> two-rotation-piece
               next-rotation-shape
               next-rotation-shape))))
  (let [four-rotation-piece {:shape :a
                             :rotations [:b :c :d]}]
    (is (= {:shape :b
            :rotations [:c :d :a]}
           (next-rotation-shape four-rotation-piece)))
    (is (= {:shape :c
            :rotations [:d :a :b]}
           (-> four-rotation-piece next-rotation-shape next-rotation-shape)))
    (is (= {:shape :d
            :rotations [:a :b :c]}
           (-> four-rotation-piece next-rotation-shape next-rotation-shape next-rotation-shape)))
    (is (= {:shape :a
            :rotations [:b :c :d]}
           (-> four-rotation-piece next-rotation-shape next-rotation-shape next-rotation-shape next-rotation-shape)))))
