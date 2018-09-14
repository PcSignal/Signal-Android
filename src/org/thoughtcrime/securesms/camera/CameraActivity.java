package org.thoughtcrime.securesms.camera;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.components.GlideDrawableListeningTarget;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.scribbles.ScribbleFragment;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.whispersystems.libsignal.util.guava.Optional;

// TODO: Extend correct superclass?
public class CameraActivity extends AppCompatActivity implements CameraFragment.Controller,
                                                                 ScribbleFragment.Controller {

  private static final String TAG = CameraActivity.class.getSimpleName();

  private static final String TAG_CAMERA    = "camera";
  private static final String TAG_EDITOR    = "editor";
  private static final String KEY_TRANSPORT = "transport";

  public static final String EXTRA_MESSAGE   = "message";
  public static final String EXTRA_TRANSPORT = "transport";
  public static final String EXTRA_WIDTH     = "width";
  public static final String EXTRA_HEIGHT    = "height";
  public static final String EXTRA_SIZE      = "size";

  private ImageView       snapshot;
  private TransportOption transportOption;
  private Uri             imageUri;
  private boolean         success;


  public static Intent getIntent(@NonNull Context context, @NonNull TransportOption transportOption) {
    Intent intent = new Intent(context, CameraActivity.class);
    intent.putExtra(KEY_TRANSPORT, transportOption);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.camera_activity);

    snapshot        = findViewById(R.id.camera_snapshot);
    transportOption = (TransportOption) getIntent().getSerializableExtra(KEY_TRANSPORT);

    if (savedInstanceState == null) {
      CameraFragment fragment = CameraFragment.newInstance();
      getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment, TAG_CAMERA).commit();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // TODO: Both images
    if (!success && imageUri != null) {
      PersistentBlobProvider.getInstance(this).delete(this, imageUri);
      imageUri = null;
    }
  }

  @Override
  public void onBackPressed() {
    ScribbleFragment fragment = (ScribbleFragment) getSupportFragmentManager().findFragmentByTag(TAG_EDITOR);
    if (fragment != null && fragment.isEmojiKeyboardVisible()) {
      fragment.dismissEmojiKeyboard();
    } else {
      // TODO: Both images
      if (imageUri != null) {
        PersistentBlobProvider.getInstance(this).delete(this, imageUri);
        imageUri = null;
      }
      super.onBackPressed();
    }
  }

  @Override
  public void onCameraError() {
    // TODO: Localize string
    Toast.makeText(this, "Camera unavailable.", Toast.LENGTH_SHORT).show();
    setResult(RESULT_CANCELED, new Intent());
    finish();
  }

  @Override
  public void onFastImageCaptured(@NonNull Uri uri) {
    imageUri = uri;

    SettableFuture<Boolean> result = new SettableFuture<>();
    GlideApp.with(this).load(new DecryptableStreamUriLoader.DecryptableUri(uri)).into(new GlideDrawableListeningTarget(snapshot, result));
    result.addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        ScribbleFragment fragment = ScribbleFragment.newInstance(uri, Optional.of(transportOption));
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.fragment_container, fragment, TAG_EDITOR)
                                   .addToBackStack(null)
                                   .commit();
      }
    });
  }

  @Override
  public void onFullImageCaptured(@NonNull Uri uri) {
    Log.e(TAG, "Full image captured: " + uri.toString());
  }

  @Override
  public int getDisplayRotation() {
    return getWindowManager().getDefaultDisplay().getRotation();
  }

  @Override
  public void onImageEditComplete(@NonNull Uri uri, int width, int height, long size, @NonNull Optional<String> message) {
    success = true;
    
    Intent intent = new Intent();
    intent.setData(uri);
    intent.putExtra(EXTRA_WIDTH, width);
    intent.putExtra(EXTRA_HEIGHT, height);
    intent.putExtra(EXTRA_SIZE, size);
    intent.putExtra(EXTRA_MESSAGE, message.or(""));
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  public void onImageEditFailure() {
    // TODO: Localize string
    Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
    finish();
  }
}
