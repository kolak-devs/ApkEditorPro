package com.gmail;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import com.gmail.heagoo.apkeditor.GlobalConfig;

public class App extends Application{
  @Override
  public void onCreate() {
    super.onCreate();
    // Apply user-selected day/night theme (System/Light/Dark)
    AppCompatDelegate.setDefaultNightMode(
        GlobalConfig.instance(this).getNightMode());
    // TODO: Implement this method
    CaocConfig.Builder.create()
    .logErrorOnRestart(true)
    .showErrorDetails(true)
    .showRestartButton(true)
    .apply();
    
  }
  
}
