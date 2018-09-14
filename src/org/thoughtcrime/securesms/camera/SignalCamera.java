package org.thoughtcrime.securesms.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.IOException;

public interface SignalCamera {

  @SuppressLint("MissingPermission")
  static SignalCamera get(int preferredDirection, @NonNull EventListener eventListener) {
    return new SignalCamera1(preferredDirection, eventListener);
  }

  void initialize();

  void release();

  void linkSurface(@NonNull SurfaceTexture surfaceTexture) throws IOException;

  void setScreenRotation(int rotation);

  void capture(@NonNull CaptureCompleteCallback callback);

  int flip();

  interface CaptureCompleteCallback {
    void onComplete(@NonNull byte[] data, int width, int height, int rotation);
  }

  class Capabilities {

    final int cameraCount;
    final int previewWidth;
    final int previewHeight;

    public Capabilities(int cameraCount, int previewWidth, int previewHeight) {
      this.cameraCount   = cameraCount;
      this.previewWidth  = previewWidth;
      this.previewHeight = previewHeight;
    }

    int getCameraCount() {
      return cameraCount;
    }
  }

  interface EventListener {
    void onCameraUnavailable();
    void onCapabilitiesAvailable(@NonNull Capabilities capabilities);
  }
}
