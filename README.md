# Solar System [ARCore Example]
This is an example application for Android SDK 24 or above that can use ARCode by Google. 

On this example the camera will detect the surface and show some white points. When you make a tap over this points this will render the Solar system including:
- Sun
- Mercury
- Venus
- Earth
- Moon
- Mars
- Jupiter
- Saturn
- Uranus
- Neptune

The renders that u need are located on the app/sampledata/models.

I had used <b>com.google.ar.sceneform:plugin:1.3.0</b> like a project dependencie for implement sceneform.

Be caution with sceneform. Sceneform is available for the following ABIs: arm64-v8a, armv7a, x86_64 and x86. This sample app enables arm64-v8a to run on devices and x86 to run on the emulator. Your application should list the ABIs most appropriate to minimize APK size (arm64-v8a recommended).

For more information about ARCore visit: https://developers.google.com/ar/
