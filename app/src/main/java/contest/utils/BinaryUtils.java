package contest.utils;

import android.content.Context;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Alex Kravchenko on 13/04/2019.
 */
public class BinaryUtils {

    public static long[] readArrayFromAssets(Context context, String assetName) {
        try {
            InputStream inputStream = context.getAssets().open(assetName);
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int size = dataInputStream.readInt();
            long array[] = new long[size];
            byte bytes[] = new byte[4 * size];
            dataInputStream.read(bytes);
            int value = 0;
            for (int i = 0; i < size; ++i) {
                value += ((bytes[4 * i] & 255) << 24) +
                        ((bytes[4 * i + 1] & 255) << 16) +
                        ((bytes[4 * i + 2] & 255) << 8) +
                        (bytes[4 * i + 3] & 255);
                array[i] = value;
            }
            inputStream.close();
            return array;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    public static long[][] readDataArrays(Context context, String baseName, int arrays, long startTime, long endTime) {
        long dateArray[] = readArrayFromAssets(context, baseName + "_0.bin");
        int start = -1;
        int end = -1;
        int beginTimeInt = (int) (startTime / 1000L);
        int endTimeInt = (int) (endTime / 1000L);
        for (int i = 0; i < dateArray.length; ++i) {
            if (dateArray[i] >= beginTimeInt && start < 0) {
                start = i;
            }
            if (dateArray[i] <= endTimeInt) {
                end = i;
            }
            if (dateArray[i] >= beginTimeInt && dateArray[i] <= endTimeInt) {
                dateArray[i] *= 1000L; // convert back to milliseconds
            }
        }
        int length = end - start + 1;
        long result[][] = new long[arrays][];
        result[0] = new long[length];
        System.arraycopy(dateArray, start, result[0], 0, length);
        for (int i = 1; i < arrays; ++i) {
            long array[] = readArrayFromAssets(context, baseName + "_" + i + ".bin");
            result[i] = new long[length];
            System.arraycopy(array, start, result[i], 0, length);
        }
        return result;
    }

}
