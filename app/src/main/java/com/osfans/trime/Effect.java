/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.osfans.trime;

import android.content.Context;
import android.content.SharedPreferences;

import android.media.AudioManager;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** 處理按鍵聲音、震動、朗讀等效果 */
public class Effect {
  private final static int MAX_VOLUME = 100;
  private int duration = 10;
  private long durationLong;
  private int volume = 100;
  private float volumeFloat;

  private final Context context;

  private boolean vibrateOn;
  private Vibrator vibrator;
  private boolean soundOn;
  private AudioManager audioManager;
  private boolean isSpeakCommit, isSpeakKey;
  private TextToSpeech mTTS;

  public Effect(Context context) {
    this.context = context;
  }

  public void reset() {
    SharedPreferences pref = Function.getPref(context);
    duration = pref.getInt("key_vibrate_duration", duration);
    durationLong = duration * 1L;
    vibrateOn = pref.getBoolean("key_vibrate", false) && (duration > 0);
    if (vibrateOn && (vibrator == null)) {
      vibrator =
        (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    volume = pref.getInt("key_sound_volume", volume);
    volumeFloat = (float) (1 - (Math.log(MAX_VOLUME - volume) / Math.log(MAX_VOLUME)));
    soundOn = pref.getBoolean("key_sound", false);
    if (soundOn && (audioManager == null)) {
      audioManager = 
        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    isSpeakCommit = pref.getBoolean("speak_commit", false);
    isSpeakKey = pref.getBoolean("speak_key", false);
    if (mTTS == null && (isSpeakCommit || isSpeakKey)) {
      mTTS = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
        public void onInit(int status) {
          //初始化結果
        }
      });
    }
  }

  public void vibrate() {
    if (vibrateOn && (vibrator != null)) vibrator.vibrate(durationLong);
  }

  public void playSound(final int code) {
    if (soundOn && (audioManager != null)) {
      final int sound;
      switch (code) {
        case KeyEvent.KEYCODE_DEL:
            sound = AudioManager.FX_KEYPRESS_DELETE;
            break;
        case KeyEvent.KEYCODE_ENTER:
            sound = AudioManager.FX_KEYPRESS_RETURN;
            break;
        case KeyEvent.KEYCODE_SPACE:
            sound = AudioManager.FX_KEYPRESS_SPACEBAR;
            break;
        default:
            sound = AudioManager.FX_KEYPRESS_STANDARD;
            break;
      }
      audioManager.playSoundEffect(sound, volumeFloat);
    }
  }

  public void setLanguage(Locale loc) {
    if (mTTS != null) mTTS.setLanguage(loc);
  }

  public void speak(CharSequence text) {
    if (text!= null && mTTS != null) mTTS.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null);
  }

  public void speakCommit(CharSequence text) {
    if (isSpeakCommit) speak(text);
  }

  public void speakKey(CharSequence text) {
    if (isSpeakKey) speak(text);
  }

  public void speakKey(int code) {
    if (code <= 0) return;
    String text = KeyEvent.keyCodeToString(code).replace("KEYCODE_","").replace("_", " ").toLowerCase(Locale.getDefault());
    speakKey(text);
  }

  public void destory() {
     if (mTTS != null) {
       mTTS.stop();
       mTTS.shutdown();
       mTTS = null;
     }
  }
}
