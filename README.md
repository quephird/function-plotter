## Description

This is a simple 3D mathematical surface plotter demonstrating some of the capabilities of quil and Processing.
Much of the code is based on an example from a presentation on Processing here: here, www.dsic.upv.es/~jlinares/grafics/processing_eng_7.pdf

## Getting it running

This project require Leiningen which can be obtained and installed from here: http://www.leiningen.org/

Download the project to a local directory and run the following:

    lein clean
    lein deps
    lein repl

Once in the Clojure REPL, issue the following:

    (load-file "./src/function_plotter/core.clj")
    (in-ns 'function-plotter.core)
    (-main)

Theoretically, you should be able to run this via lein run or building an uberjar and running it, but it does not work.
More on this below.

## Current features

* Moving of the camera using the mouse
* Zooming in and out using the mouse wheel
* Toggling of the gridlines and the surface fill

## Caveats

Zooming out too far results in some clipping; I need to look into this.

Currently, I cannot figure out why running the applet the first time or so, on Windows at least, results in the following error:

    Exception in thread "Animation Thread" javax.media.opengl.GLException: Can not d
    estroy context while it is current
            at com.sun.opengl.impl.GLContextImpl.destroy(GLContextImpl.java:176)
            at processing.opengl.PGraphicsOpenGL.allocate(PGraphicsOpenGL.java:294)
            at processing.core.PGraphics3D.setSize(Unknown Source)
            at processing.core.PApplet.resizeRenderer(Unknown Source)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.resizeRender
    er(Unknown Source)
            at processing.core.PApplet.size(Unknown Source)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.size(Unknown
    Source)
            at processing.core.PApplet.size(Unknown Source)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.size(Unknown
    Source)
            at quil.applet$applet_set_size.invoke(applet.clj:131)
            at clojure.lang.AFn.applyToHelper(AFn.java:167)
            at clojure.lang.AFn.applyTo(AFn.java:151)
            at clojure.core$apply.invoke(core.clj:601)
            at quil.applet$applet$setup_fn__76.invoke(applet.clj:238)
            at quil.applet$applet$fn__168.invoke(applet.clj:348)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.setup(Unknow
    n Source)
            at processing.core.PApplet.handleDraw(Unknown Source)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.handleDraw(U
    nknown Source)
            at processing.core.PApplet.run(Unknown Source)
            at quil.applet.proxy$processing.core.PApplet$IMeta$c506c738.run(Unknown
    Source)
            at java.lang.Thread.run(Unknown Source)

This applet ought to be run from a uberjar file instead of being burdened with the multiple steps above, which is why I bothered to create a -main function and the defsketch inside of it.
If anyone has any ideas on what I'm doing wrong here or how to workaround what appears to be a bug, I'm all ears.

## Goals

* User input of mathematical function to be plotted
* User input of x and y ranges
* User input of the surface color
* Ability to tweak the number of plot points

## Useful links

The fabulous quil library written in Clojure:
https://github.com/quil/quil

The equally kewl Processing environment:
http://www.processing.org/

## License

Copyright (C) 2012, pɹoɟɟǝʞ uɐp
Distributed under the Eclipse Public License.