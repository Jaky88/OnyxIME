/*
 * Copyright 2015 osfans
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.osfans.trime;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.os.Build;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.app.SearchManager;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.lang.Exception;

/** 實現打開指定程序、打開{@link Pref 輸入法全局設置}對話框等功能 */
public class Function {
  private static String TAG = "Function";
  static SparseArray<String> sApplicationLaunchKeyCategories;
  static {
    sApplicationLaunchKeyCategories = new SparseArray<String>();
    sApplicationLaunchKeyCategories.append(
            KeyEvent.KEYCODE_EXPLORER, "android.intent.category.APP_BROWSER");
    sApplicationLaunchKeyCategories.append(
            KeyEvent.KEYCODE_ENVELOPE, "android.intent.category.APP_EMAIL");
    sApplicationLaunchKeyCategories.append(
            207, "android.intent.category.APP_CONTACTS");
    sApplicationLaunchKeyCategories.append(
            208, "android.intent.category.APP_CALENDAR");
    sApplicationLaunchKeyCategories.append(
            209, "android.intent.category.APP_EMAIL");
    sApplicationLaunchKeyCategories.append(
            210, "android.intent.category.APP_CALCULATOR");
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
  public static boolean openCategory(Context context, int keyCode) {
    String category = sApplicationLaunchKeyCategories.get(keyCode);
    if (category != null) {
      Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
      try {
          context.startActivity(intent);
      } catch (Exception ex) {
      }
      return true;
    }
    return false;
  }

  public static void startIntent(Context context, String arg) {
    Intent intent;
    try {
      if (arg.indexOf(':') >= 0) {
        // The argument is a URI.  Fully parse it, and use that result
        // to fill in any data not specified so far.
        intent = Intent.parseUri(arg, Intent.URI_INTENT_SCHEME);
      } else if (arg.indexOf('/') >= 0) {
        // The argument is a component name.  Build an Intent to launch
        // it.
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(ComponentName.unflattenFromString(arg));
      } else {
        // Assume the argument is a package name.
        intent = context.getPackageManager().getLaunchIntentForPackage(arg);
      }
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
      context.startActivity(intent);
    } catch (Exception ex) {
    }
  }

  public static void startIntent(Context context, String action, String arg) {
    action = "android.intent.action." + action.toUpperCase(Locale.getDefault());
    try {
      Intent intent = new Intent(action);
      switch (action) {
        case Intent.ACTION_WEB_SEARCH:
        case Intent.ACTION_SEARCH:
          if (arg.startsWith("http")) { //web_search無法直接打開網址
            startIntent(context, arg);
            return;
          }
          intent.putExtra(SearchManager.QUERY, arg);
          break;
        default:
          if (!isEmpty(arg)) intent.setData(Uri.parse(arg));
          break;
      }
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
      context.startActivity(intent);
    } catch (Exception ex) {
    }
  }

  public static void showPrefDialog(Context context) {
    Intent intent = new Intent(context, Pref.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
    context.startActivity(intent);
  }

  public static String handle(Context context, String command, String option) {
    String s = null;
    if (command == null) return s;
    switch (command) {
      case "date":
        s = new SimpleDateFormat(option, Locale.getDefault()).format(new Date()); //時間
        break;
      case "run":
        startIntent(context, option); //啓動程序
        break;
      default:
        startIntent(context, command, option); //其他intent
        break;
    }
    return s;
  }

  public static boolean isEmpty(CharSequence s) {
    return (s == null) || (s.length() == 0);
  }

  public static String getString(Map m, String k) {
    if (m.containsKey(k)) {
      Object o = m.get(k);
      if (o != null) return o.toString();
    }
    return "";
  }

  public static void check() {
    Rime.check(true);
    System.exit(0); //清理內存
  }

  public static void deploy() {
    Rime.destroy();
    Rime.get(true);
    //Trime trime = Trime.getService();
    //if (trime != null) trime.invalidate();
  }

  public static void sync() {
    Rime.syncUserData();
  }

  public static String getVersion(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isAppAvailable(Context context, String app) {
    final PackageManager packageManager = context.getPackageManager();
    List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
    if (pinfo != null) {
        for (int i = 0; i < pinfo.size(); i++) {
            String pn = pinfo.get(i).packageName;
            if (pn.equals(app)) {
                return true;
            }
        }
    }
    return false;
  }

  public static SharedPreferences getPref(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static boolean isDiffVer(Context context) {
    String version = getVersion(context);
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    String pref_ver = pref.getString("version_name", "");
    boolean isDiff = !version.contentEquals(pref_ver);
    if (isDiff) {
      SharedPreferences.Editor edit = pref.edit();
      edit.putString("version_name", version);
      edit.apply();
    }
    return isDiff;
  }
}
