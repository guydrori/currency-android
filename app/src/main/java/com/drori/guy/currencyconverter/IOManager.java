package com.drori.guy.currencyconverter;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;


class IOManager {
    private final File file;
    private HashMap<String, Double> inputMap;
    private Date inputDate;
    public IOManager (String filename) {
        file = new File(MainActivity.main.getApplicationContext().getFilesDir(),filename);
    }
    public void rewriteAndSave(Object object) {
        ObjectOutputStream outputStream;
        try {
            if (file.exists()) {
                outputStream = new ObjectOutputStream(new FileOutputStream(file, false));
            } else {
                outputStream = new ObjectOutputStream(new FileOutputStream(file));
            }
            outputStream.writeObject(object);
            Date date = new Date(System.currentTimeMillis());
            outputStream.writeObject(date);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Log.e("I/O", "File writing failed");
            e.printStackTrace();
        }
    }
    public void readFile() throws Exception {
        ObjectInputStream inputStream;
        inputStream = new ObjectInputStream(new FileInputStream(file));
        inputMap = (HashMap) inputStream.readObject();
        inputDate = (Date) inputStream.readObject();
    }
    public HashMap<String,Double> getRatesMap()  {
        return inputMap;
    }
    public Date getDate () {
        return inputDate;
    }
    public boolean readable() {
        return file.exists() && !file.isDirectory() && file.canRead();
    }

}
