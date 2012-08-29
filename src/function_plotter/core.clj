(ns function-plotter.core
  (:gen-class)
  (:import [processing.core PConstants]
           [java.awt.event ActionListener MouseWheelListener]
           [java.lang Runnable]
           [javax.swing JButton JCheckBox JFrame JPanel JSpinner SpinnerNumberModel]
           [javax.swing.event ChangeListener])
  (:use quil.core quil.applet)
  )

(def screen-w 500)
(def screen-h 500)

(def scale-z 600)

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

(def grid-on (atom true))
(def grid-color (atom [0 127 0]))
(def fill-on (atom true))
(def fill-color (atom [127 0 127]))

(def steps (atom 31))


(defn f [x y]
  (Math/sin (+ (* x x) (* y y)))
;  (* 2 (Math/sin (* (+ (* x x) (* y y)) PConstants/PI)) (Math/exp (- (+ (* x x) (* y y)))))
  )


(defn- compute-points []
  (let [range-x (- end-x start-x)
        range-y (- end-y start-y)]
    (reset! zs
      (into []
        (for [i (range (inc @steps))
              j (range (inc @steps))]
          (f (+ start-x (* i (/ range-x @steps)))
             (+ start-y (* j (/ range-y @steps)))))
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

(defn- check-grid []
  (if @grid-on
    (apply stroke @grid-color)
    (no-stroke)))

(defn- check-fill []
  (if @fill-on
    (apply fill @fill-color)
    (no-fill)))

(defn setup []
  (smooth)
  (frame-rate 30)
  (compute-points)
  )

(defn- draw-function-plot []
  (check-grid)
  (check-fill)

  (let [range-x (- end-x start-x)
        range-y (- end-y start-y)]
    (doseq [j (range @steps)]
      (begin-shape :quad-strip)
      (doseq [i (range (inc @steps))]
        (let [x (+ start-x (* i (/ range-x @steps)))
              y (+ start-y (* j (/ range-y @steps)))]
          (vertex x y (@zs (+ i (* j (inc @steps)))))
          (vertex x (+ y (/ range-y @steps)) (@zs (+ i (* (inc j) (inc @steps)))))
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
  (lights)
  (background 0)
  ; Center the results on window
  (translate (/ screen-w 2) (/ screen-h 2) @zoom-z)

  ; Rotate the camera
  (rotate-y (+ @rot-y @dist-x))
  (rotate-x (+ @rot-x @dist-y))

  ; Centering around (0, 0);
  (translate (- (/ screen-w 2)) (- (/ screen-h 2)))

  ; NOTA BENE: quil only exposes two arities of scale so we need to dive into Java here.
  (.scale (current-applet) screen-w screen-h scale-z)

  (draw-function-plot)
  (draw-axes)
  )

(defn -main [& args]
  ; I need to get a hold of the raw PApplet object here
  ; in order to be able to add to a JPanel.
  ; And I need to dump into Swing in the first place
  ; because Processing does not come bundled with any widgets
  ; and openly discourages adding AWT or Swing components
  ; to PApplets. There are a few Processing libraries that
  ; have been developed to allow for direct addition of widgets
  ; to PApplets, but alas... there is no way to automate inclusion
  ; of those dependencies via Processing or Leiningen. Ugh.
  ;
  ; TODO: Take a look at Seesaw. There is waaaay too much ceremony here.
  ;       In the mean time, IT FRIGGIN' WORKS.
  (let [plotter (applet :setup setup
                        :draw draw
                        :renderer :opengl
                        :mouse-pressed mouse-pressed
                        :mouse-dragged mouse-dragged
                        :mouse-released mouse-released
                        :size [500 500]
                        :target :none
                  )
        mouse-wheel-listener (proxy [MouseWheelListener] []
                               (mouseWheelMoved [mwe]
                                 (mouse-wheel (.getWheelRotation mwe))))
        fill-toggle (JCheckBox. "Toggle fill" @fill-on)
        fill-toggle-listener (proxy [ActionListener] []
                               (actionPerformed [ae]
                                 (swap! fill-on not)
                                 (.redraw plotter)))
        grid-toggle (JCheckBox. "Toggle grid" @grid-on)
        grid-toggle-listener (proxy [ActionListener] []
                               (actionPerformed [ae]
                                 (swap! grid-on not)
                                 (.redraw plotter)))
        plot-point-model (SpinnerNumberModel. @steps 0 100 1)
        plot-point-spinner (JSpinner. plot-point-model)
        ; TODO: Figure out how to insure that the applet stops drawing
        ;       before mutating steps and recomputing zs.
        ;       As is, there are spurious IndexOutOfBoundsExceptions.
        plot-point-listener (proxy [ChangeListener] []
                              (stateChanged [ce]
                                (.stop plotter)
                                (reset! steps (.getNumber plot-point-model))
                                (compute-points)
                                (.start plotter)))
        panel (JPanel.)
        frame (JFrame. "Function plotter")
        ]
    (doto plotter
      (.addMouseWheelListener mouse-wheel-listener))
    (doto fill-toggle
      (.addActionListener fill-toggle-listener))
    (doto grid-toggle
      (.addActionListener grid-toggle-listener))
    (doto plot-point-spinner
      (.addChangeListener plot-point-listener))
    (doto panel
      (.add plotter)
      (.add fill-toggle)
      (.add grid-toggle)
      (.add plot-point-spinner))
    (doto frame
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.add panel)
      (.setSize 800 550)
      (.setVisible true))
    )
  )
