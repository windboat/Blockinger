package org.blockinger.game;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.blockinger.game.components.GameState;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GameStateSerialHelper {
    private static final boolean DEBUG = false;

    private static String filename = "gameStateProxy.obj";

    private static GameState.GameStateProxy gameStateProxy;

    public static synchronized boolean saveGameState(Context context, GameState game) {
        boolean flag = false;
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE));
            if (game == null || !game.isResumable()) {
                out.writeObject(null);
                gameStateProxy = null;
            } else {
                out.writeObject(game);
                gameStateProxy = new GameState.GameStateProxy(game);
            }
            out.flush();
            flag = true;
        } catch (IOException e) {
            Log.e("GameStateSerialHelper", "saveGameState failed.", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return flag;
    }

    public static synchronized GameState.GameStateProxy readGameState(Context context) {
        if (gameStateProxy != null) {
            if(DEBUG) Log.i("GameStateSerialHelper", "readGameState() return obj: " + gameStateProxy);
            return gameStateProxy;
        }
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(context.openFileInput(filename));
            gameStateProxy = (GameState.GameStateProxy) in.readObject();
        } catch (Exception e) {
            Log.d("GameStateSerialHelper", "readGameState failed.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        if(DEBUG) Log.i("GameStateSerialHelper", "readGameState() read obj: " + gameStateProxy);
        return gameStateProxy;
    }

    public static boolean isResumable2(Context context) {
//        Log.i("GameStateSerialHelper", context.getDatabasePath(filename).getAbsolutePath());
//        Log.i("GameStateSerialHelper", context.getFilesDir().getAbsolutePath());
        File file = context.getFilesDir();
        if (file != null) {
            file = new File(file, filename);
        }
        boolean ret = file.exists() && file.isFile();
//        Log.i("GameStateSerialHelper", "isResumable() " + ret + ", path: " + file.getAbsolutePath());
        return ret;
    }

    public static boolean isResumable(Context context) {
        boolean ret = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_serial_game", false);
//        Log.i("GameStateSerialHelper", "isResumable() " + ret );
        return ret;
    }

}
