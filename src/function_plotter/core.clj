(ns function-plotter.core
  (:gen-class)
  (:import [processing.core PConstants]
           [java.awt.event MouseWheelListener])
  (:use quil.core quil.applet)
  )

(def screen-w 500)
(def screen-h 500)

(def steps 50)
(def scale-z 200)

(def start-x -3.0)
(def end-x 3.0)
(def start-y -3.0)
(def end-y 3.0)

(def zs (atom []))

(def rot-x (atom 0.0))
(def rot-y (atom 0.0))
(def last-x (atom 0.0))
(def last-y (atom 0.0))
(def dist-x (atom 0.0))
(def dist-y (atom 0.0))
;(def zoom-z -300)
(def zoom-z (atom -2000))

(defn f [x y]
  (Math/sin (+ (* x x) (* y y))))

(defn- compute-points []
  (let [range-x (- end-x start-x)
        range-y (- end-y start-y)]
    (reset! zs
      (into []
        (for [i (range (inc steps))
              j (range (inc steps))]
          (f (+ start-x (* i (/ range-x steps)))
             (+ start-y (* j (/ range-y steps)))))
        )
      )
    )
  )
(defn- mouse-pressed []
  (reset! last-x (mouse-x))
  (reset! last-y (mouse-y))
  )

(defn- mouse-dragged []
  (reset! dist-x (radians (- (mouse-x) @last-x)))
  (reset! dist-y (radians (- @last-y (mouse-y))))
  )

(defn- mouse-released []
  (swap! rot-x + @dist-y)
  (swap! rot-y + @dist-x)
  (reset! dist-x 0.0)
  (reset! dist-y 0.0)
  )

(defn- mouse-wheel [mwr]
  (if (> mwr 0)
    (swap! zoom-z + 50)
    (swap! zoom-z - 50))
  )

(defn setup []
  (smooth)
  (no-fill)
  (let [applet (current-applet)
        mouse-wheel-listener (proxy [java.awt.event.MouseWheelListener] []
                               (mouseWheelMoved [mwe] (mouse-wheel (.getWheelRotation mwe)))
                               )]
    (.addMouseWheelListener applet mouse-wheel-listener)
    )
  (compute-points)
  )

(defn- draw-function-plot []
  (lights)
  (stroke 255)
  (no-stroke)
  (fill 127 127 0)

  (let [range-x (- end-x start-x)
        range-y (- end-y start-y)]
    (doseq [j (range steps)]
      (begin-shape :quad-strip)
      (doseq [i (range (inc steps))]
        (let [x (+ start-x (* i (/ range-x steps)))
              y (+ start-y (* j (/ range-y steps)))]
          (vertex x y (@zs (+ i (* j (inc steps)))))
          (vertex x (+ y (/ range-y steps)) (@zs (+ i (* (inc j) (inc steps)))))
          )
        )
      (end-shape)
      )
    )
  )

(defn- draw-axes []
  (stroke 255 0 0)
  (line -2000 0 0 2000 0 0)
  (stroke 0 255 0)
  (line 0 -2000 0 0 2000 0)
  (stroke 0 0 255)
  (line 0 0 -2000 0 0 2000)
  )

(defn draw []
  (background 0)
  ; We center the results on window
  (translate (/ screen-w 2) (/ screen-h 2) @zoom-z)

  ; Rotation
  (rotate-y (+ @rot-y @dist-x))
  (rotate-x (+ @rot-x @dist-y))

  ; Centering around (0, 0);
  (translate (- (/ screen-w 2)) (- (/ screen-h 2)))

  ; Function covers
  ; 400 x 400 x scaleZ
  ; NOTA BENE: quil only exposes two arities of scale so we
  ;            need to dive into Java here.
  (.scale (current-applet) screen-w screen-h scale-z)
  ;(scale screen-w screen-h scale-z)

  (draw-function-plot)
  (draw-axes)
  )

(defn -main [& args]
  (defsketch plotter
    :title "function plot"
    :setup setup
    :draw draw
    :renderer :opengl
    :mouse-pressed mouse-pressed
    :mouse-dragged mouse-dragged
    :mouse-released mouse-released
    :size [500 500]
    )
  )

