package com.example.clonemdp;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BluetoothService extends Service {

    private final IBinder binder = new LocalBinder();
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread listenThread;

    private final List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** Set the connected socket */
    public void setSocket(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        startListening();
    }

    /** Send a message through the socket */
    public void sendMessage(String message) {
        if (socket != null && outputStream != null) {
            new Thread(() -> {
                try {
                    outputStream.write(message.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /** Register activity listener */
    public void registerListener(MessageListener listener) {
        listeners.add(listener);
    }

    /** Unregister listener */
    public void unregisterListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /** Notify UI of incoming messages */
    private void notifyListeners(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (MessageListener listener : listeners) {
                listener.onMessageReceived(message);
            }
        });
    }

    /** Background thread to listen for incoming messages */
    private void startListening() {
        listenThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            try {
                while (socket != null && inputStream != null) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String msg = new String(buffer, 0, bytes);
                        notifyListeners(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenThread.start();
    }
}
