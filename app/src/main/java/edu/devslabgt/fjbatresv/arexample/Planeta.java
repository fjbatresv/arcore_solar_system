package edu.devslabgt.fjbatresv.arexample;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

public class Planeta extends Node implements Node.OnTapListener {
    private final String nombre;
    private final float scale;
    private final ModelRenderable renderable;
    private final SolarSettings solarSettings;

    private Node infoCard;
    private RotatingNode planetVisual;
    private final Context context;

    private static final float INFO_CARD_Y_POS_COEFF = 0.55f;

    public Planeta(String nombre, float scale, ModelRenderable renderable, SolarSettings solarSettings, Context context) {
        this.nombre = nombre;
        this.scale = scale;
        this.renderable = renderable;
        this.solarSettings = solarSettings;
        this.context = context;
    }

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void onActivate() {

        if (getScene() == null) {
            throw new IllegalStateException("Scene is null!");
        }

        if (infoCard == null) {
            infoCard = new Node();
            infoCard.setParent(this);
            infoCard.setEnabled(false);
            infoCard.setLocalPosition(new Vector3(0.0f, scale * INFO_CARD_Y_POS_COEFF, 0.0f));

            ViewRenderable.builder()
                    .setView(context, R.layout.planet_card_view)
                    .build()
                    .thenAccept(
                            (renderable) -> {
                                infoCard.setRenderable(renderable);
                                TextView textView = (TextView) renderable.getView();
                                textView.setText(nombre);
                            })
                    .exceptionally(
                            (throwable) -> {
                                throw new AssertionError("Could not load plane card view.", throwable);
                            });
        }

        if (planetVisual == null) {
            planetVisual = new RotatingNode(solarSettings, false);
            planetVisual.setParent(this);
            planetVisual.setRenderable(renderable);
            planetVisual.setLocalScale(new Vector3(scale, scale, scale));
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        if (infoCard == null) {
            return;
        }

        infoCard.setEnabled(!infoCard.isEnabled());
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (infoCard == null) {
            return;
        }

        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (getScene() == null) {
            return;
        }
        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 cardPosition = infoCard.getWorldPosition();
        Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        infoCard.setWorldRotation(lookRotation);
    }
}
