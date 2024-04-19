# A repository containing experiments to use StarDist3d algorithms from Java 

During a Hackathon I experimented a bit with JDLL and Stardist 3D from Java

This repo contains a demo similar to (https://github.com/bioimage-io/JDLL/blob/main/src/main/java/io/bioimage/modelrunner/model/Stardist3D.java) here: https://github.com/stefanhahmann/stardist3d-java/blob/main/src/test/java/net/stefanhahmann/stardist3d/jdll/StarDist3dJdll.java The demo here already contains some demo data downloaded from the bio image model zoo in the repo so that it should directly show a result similar to this:

![grafik](https://github.com/stefanhahmann/stardist3d-java/assets/10515534/d56b2295-b2aa-4612-a5cc-6b9442a48f30)
(BDV with the input on left and BDV with the prediction on the right):

The [demo](https://github.com/stefanhahmann/stardist3d-java/blob/main/src/test/java/net/stefanhahmann/stardist3d/jdll/StarDist3dJdll.java) includes the prediction and the postprocessing of results from stardist. Both is done within JDLL/Python. It does not yet cover the preprocessing. However, there are plans that this will be integrated into JDLL soon.

There is also another Demo, which just runs the prediction without the postprocessing here: https://github.com/stefanhahmann/stardist3d-java/blob/main/src/test/java/net/stefanhahmann/stardist3d/StarDist3DDemo.java
This is added to this repo for completeness. There is also a StarConvexPolyhedron class (https://github.com/stefanhahmann/stardist3d-java/blob/main/src/main/java/net/stefanhahmann/polyhedron/StarConvexPolyhedron.java) in this repo, which could be used to implement the postprocessing on the Java side (if somebody is interested to do so).

In order to demonstrate Kotlin working together with Java within the same project / repo, there is some code translated to Kotlin: https://github.com/stefanhahmann/stardist3d-java/blob/main/src/main/kotlin/net/stefanhahmann/polyhedron/kotlin/StarConvexPolyhedron.kt
