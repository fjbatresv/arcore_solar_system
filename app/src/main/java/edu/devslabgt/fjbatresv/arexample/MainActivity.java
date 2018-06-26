package edu.devslabgt.fjbatresv.arexample;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.support.design.widget.Snackbar;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int RC_PERMISSIONS = 0x123;
    private boolean installRequested;

    private GestureDetector gestureDetector;
    private Snackbar loadingMessageSnackbar;

    private ArSceneView arSceneView;

    private ModelRenderable sun;
    private ModelRenderable mecurio;
    private ModelRenderable venus;
    private ModelRenderable tierra;
    private ModelRenderable marte;
    private ModelRenderable luna;
    private ModelRenderable jupiter;
    private ModelRenderable saturno;
    private ModelRenderable urano;
    private ModelRenderable neptuno;
    private ViewRenderable solarControls;
    private final SolarSettings settings = new SolarSettings();
    private boolean cargado = false;
    private boolean posicionado = false;
    private static final float AUT_TO_METERS = 0.5f;

    CompletableFuture<ModelRenderable> solStage;
    CompletableFuture<ModelRenderable> mercurioStage;
    CompletableFuture<ModelRenderable> venusStage;
    CompletableFuture<ModelRenderable> tierraStage;
    CompletableFuture<ModelRenderable> lunaStage;
    CompletableFuture<ModelRenderable> marteStage;
    CompletableFuture<ModelRenderable> jupiterStage;
    CompletableFuture<ModelRenderable> saturnoStage;
    CompletableFuture<ModelRenderable> uranoStage;
    CompletableFuture<ModelRenderable> neptunoStage;
    CompletableFuture<ViewRenderable> solarControlsStage;


    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Utils.checkIsSupportedDeviceOrFinish(this)){
            return;
        }
        setContentView(R.layout.activity_main);
        arSceneView = findViewById(R.id.ar_scene_view);
        initRenders();
        CompletableFuture.allOf(
                solStage, mercurioStage, venusStage, tierraStage, lunaStage, marteStage,
                jupiterStage, saturnoStage, uranoStage, neptunoStage, solarControlsStage
        ).handle((notUsed, throwable) -> {
            if (throwable != null){
                Utils.displayError(this, "No se pudo renderizar", throwable);
                return null;
            }
            try{
                sun = solStage.get();
                mecurio = mercurioStage.get();
                venus = venusStage.get();
                tierra = tierraStage.get();
                luna = lunaStage.get();
                marte = marteStage.get();
                jupiter = jupiterStage.get();
                saturno = saturnoStage.get();
                urano = uranoStage.get();
                neptuno = neptunoStage.get();
                solarControls = solarControlsStage.get();

                cargado = true;
            } catch (InterruptedException | ExecutionException e) {
                Utils.displayError(this, "No se pudo cargar el render", e);
            }
            return null;
        });
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
        arSceneView.getScene().setOnTouchListener(((hitTestResult, motionEvent) -> {
            if (!posicionado){
                return gestureDetector.onTouchEvent(motionEvent);
            }
            return false;
        }));
        arSceneView.getScene().setOnUpdateListener(frameTime -> {
            if (loadingMessageSnackbar == null){
                return;
            }

            Frame frame = arSceneView.getArFrame();

            if (frame == null){
                return;
            }

            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING){
                return;
            }

            for (Plane plane : frame.getUpdatedTrackables(Plane.class)){
                if (plane.getTrackingState() == TrackingState.TRACKING){
                    hideLoadinMessage();
                }
            }

        });

        Utils.requestCameraPermission(this, RC_PERMISSIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arSceneView == null){
            return;
        }
        if (arSceneView.getSession() == null){
            try{
                Session session =Utils.createArSession(this, installRequested);
                if (session == null){
                    installRequested = Utils.hasCameraPermission(this);
                    return;
                }else{
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException ex) {
                Utils.handleSessionException(this, ex);
            }
        }
        try{
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            Utils.displayError(this, "Camara no disponible", e);
            finish();
            return;
        }
        if (arSceneView.getSession() != null){
            showLoadingMessage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arSceneView != null){
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null){
            arSceneView.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!Utils.hasCameraPermission(this)){
            if (!Utils.shouldShowRequestPermissionRationale(this)){
                Utils.launchPermissionSettings(this);
            }else{
                Toast.makeText(this, "Necesitamos usar la camara para la experiencia", Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                    View.SYSTEM_UI_FLAG_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void onSingleTap(MotionEvent tap){
        if (!cargado){
            return;
        }
        Frame frame = arSceneView.getArFrame();
        if (frame != null){
            if (!posicionado && tryPlaceSolarSystem(tap, frame)){
                posicionado = true;
            }
        }
    }

    private boolean tryPlaceSolarSystem(MotionEvent tap, Frame frame) {
        if (tap != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING){
            for (HitResult hit : frame.hitTest(tap)){
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())){
                    Anchor anchor = hit.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arSceneView.getScene());
                    Node solarSystem = createSolarSystem();
                    anchorNode.addChild(solarSystem);
                    return true;
                }
            }
        }
        return false;
    }

    private Node createSolarSystem() {
        Node base = new Node();
        Node sun = new Node();
        sun.setParent(base);
        sun.setLocalPosition(new Vector3(0.0f, 0.5f, 0.0f));

        Node sunVisual = new Node();
        sunVisual.setParent(sun);
        sunVisual.setRenderable(this.sun);
        sunVisual.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));

        Node solarControls = new Node();
        solarControls.setParent(sun);
        solarControls.setRenderable(this.solarControls);
        solarControls.setLocalPosition(new Vector3(0.0f, 0.25f, 0.0f));

        View solarControlView = this.solarControls.getView();
        SeekBar orbitSpeedBar = solarControlView.findViewById(R.id.orbitSpeedBar);
        orbitSpeedBar.setProgress((int)(settings.getOrbitSpeedMultiplier() * 10.0f));
        orbitSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / (float) orbitSpeedBar.getMax();
                settings.setOrbitSpeedMultiplier(ratio * 10.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar rotationSpeedBar = solarControlView.findViewById(R.id.rotationSpeedBar);
        rotationSpeedBar.setProgress((int)(settings.getRotationSpeedMultiplier() * 10.0f));
        rotationSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / (float) rotationSpeedBar.getMax();
                settings.setRotationSpeedMultiplier(ratio * 10.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sunVisual.setOnTapListener(((hitTestResult, motionEvent) -> {
            solarControls.setEnabled(!solarControlView.isEnabled());
        }));

        createPlanet("Mercurio", sun, 0.4f, 47f, mecurio, 0.019f);
        createPlanet("Venus", sun, 0.7f, 35f, venus, 0.0475f);
        createPlanet("Tierra", sun, 1.0f, 29f, tierra, 0.05f);
        createPlanet("Luna", sun, 0.15f, 100f, luna, 0.018f);
        createPlanet("Marte", sun, 1.5f, 24f, marte, 0.0265f);
        createPlanet("Jupiter", sun, 2.2f, 13f, jupiter, 0.16f);
        createPlanet("Saturno", sun, 3.5f, 9f, saturno, 0.1325f);
        createPlanet("Urano", sun, 5.2f, 7f, urano, 0.1f);
        createPlanet("Neptuno", sun, 6.1f, 5f, neptuno, 0.074f);

        return base;
    }

    private Node createPlanet(String name, Node sun, float v, float v1, ModelRenderable renderable, float v2) {
        RotatingNode orbit = new RotatingNode(settings, true);
        orbit.setDegreesPerSecond(v1);
        orbit.setParent(sun);

        // Create the planet and position it relative to the sun.
        Planeta planet = new Planeta(name, v2, renderable, settings, this);
        planet.setParent(orbit);
        planet.setLocalPosition(new Vector3(v * AUT_TO_METERS, 0.0f, 0.0f));

        return planet;
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }
        loadingMessageSnackbar =
                Snackbar.make(
                        MainActivity.this.findViewById(android.R.id.content),
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadinMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    private void initRenders() {
        solStage = ModelRenderable.builder().setSource(this, Uri.parse("Sol.sfb")).build();
        mercurioStage = ModelRenderable.builder().setSource(this, Uri.parse("Mercury.sfb")).build();
        venusStage = ModelRenderable.builder().setSource(this, Uri.parse("Venus.sfb")).build();
        tierraStage = ModelRenderable.builder().setSource(this, Uri.parse("Earth.sfb")).build();
        lunaStage = ModelRenderable.builder().setSource(this, Uri.parse("Luna.sfb")).build();
        marteStage = ModelRenderable.builder().setSource(this, Uri.parse("Mars.sfb")).build();
        jupiterStage = ModelRenderable.builder().setSource(this, Uri.parse("Jupiter.sfb")).build();
        saturnoStage = ModelRenderable.builder().setSource(this, Uri.parse("Saturn.sfb")).build();
        uranoStage = ModelRenderable.builder().setSource(this, Uri.parse("Uranus.sfb")).build();
        neptunoStage = ModelRenderable.builder().setSource(this, Uri.parse("Neptune.sfb")).build();
        solarControlsStage = ViewRenderable.builder().setView(this, R.layout.solar_controls).build();
    }
}
