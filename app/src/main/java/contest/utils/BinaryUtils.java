package contest.utils;

import android.content.Context;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Alex Kravchenko on 13/04/2019.
 */
public class BinaryUtils {

    public static int[] readArrayFromAssets(Context context, String assetName) {
        try {
            InputStream inputStream = context.getAssets().open(assetName);
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int size = dataInputStream.readInt();
            int array[] = new int[size];
            byte bytes[] = new byte[4 * size];
            dataInputStream.read(bytes);
            int value = 0;
            for (int i = 0; i < size; ++i) {
                value += ((int)bytes[4 * i] << 24) +
                        ((bytes[4 * i + 1]) << 16) +
                        ((bytes[4 * i + 2]) << 8) +
                        (bytes[4 * i + 3]);
                array[i] = value;
            }
            inputStream.close();
            return array;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
