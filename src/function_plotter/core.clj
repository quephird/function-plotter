(ns function-plotter.core
  (:gen-class)
  (:import [processing.core PConstants]
           [java.awt GridBagConstraints GridBagLayout GridLayout]
           [java.awt.event ActionListener MouseWheelListener]
           [java.lang Runnable]
           [javax.swing JButton JCheckBox JFrame JLabel JPanel JSpinner SpinnerNumberModel]
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

; These two entities are tightly coupled and their mutations need to be coordinated;
; need to use refs instead of atoms here.
(def steps (ref 31))
(def zs (ref []))

(defn f [x y]
  (Math/sin (+ (* x x) (* y y)))
;  (* 2 (Math/sin (* (+ (* x x) (* y y)) PConstants/PI)) (Math/exp (- (+ (* x x) (* y y)))))
  )


(defn- compute-points []
  (let [range-x (- end-x start-x)
        range-y (- end-y start-y)]
    (dosync
      (ref-set zs
        (into []
          (for [i (range (inc @steps))
                j (range (inc @steps))]
            (f (+ start-x (* i (/ range-x @steps)))
               (+ start-y (* j (/ range-y @steps)))))
          )
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

  (dosync
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

; This is a convenience method
(defn- set-x-and-y! [gbc grid-x grid-y]
  (set! (. gbc gridx) grid-x)
  (set! (. gbc gridy) grid-y)
  gbc
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
                        :size [screen-w screen-h]
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
        plot-point-label (JLabel. "Plot points")
        plot-point-model (SpinnerNumberModel. @steps 1 100 1)
        plot-point-spinner (JSpinner. plot-point-model)
        plot-point-listener (proxy [ChangeListener] []
                              (stateChanged [ce]
                                (.stop plotter)
                                (dosync
                                  (ref-set steps (.getNumber plot-point-model))
                                  (compute-points))
                                (.start plotter)))
        left-panel (JPanel.)
        right-panel (JPanel.)
        layout-constraints (GridBagConstraints.)
        main-layout (GridBagLayout.)
        widget-layout (GridBagLayout.)
        frame (JFrame. "Function plotter")
        ]
    (set! (. layout-constraints fill) GridBagConstraints/HORIZONTAL)
    (doto plotter
      (.addMouseWheelListener mouse-wheel-listener))
    (doto fill-toggle
      (.addActionListener fill-toggle-listener))
    (doto grid-toggle
      (.addActionListener grid-toggle-listener))
    (doto plot-point-spinner
      (.addChangeListener plot-point-listener))
    (doto left-panel
      (.add plotter))
    (doto right-panel
      (.setLayout widget-layout)
      (.add plot-point-label (set-x-and-y! layout-constraints 0 0))
      (.add plot-point-spinner (set-x-and-y! layout-constraints 1 0))
      (.add fill-toggle (set-x-and-y! layout-constraints 0 1))
      (.add grid-toggle (set-x-and-y! layout-constraints 1 1))
      )
    (doto frame
      (.setLayout main-layout)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.add left-panel (set-x-and-y! layout-constraints 0 0))
      (.add right-panel (set-x-and-y! layout-constraints 1 0))
;      (.setSize 850 550)
      (.pack)
      (.setVisible true))
    )
  )
