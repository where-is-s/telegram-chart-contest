package contest.utils;

import android.content.Context;
import android.util.LruCache;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Alex Kravchenko on 13/04/2019.
 */
public class BinaryUtils {

    private static LruCache<String, long[]> fileCache = new LruCache<>(20);

    public static long[] readArrayFromAssets(Context context, String assetName) {
        long cached[] = fileCache.get(assetName);
        if (cached != null) {
            return cached;
        }
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
            fileCache.put(assetName,  array);
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
            } else {
                break;
            }
        }
        if (start == -1) {
            return new long[arrays][];
        }
        if (end == -1) {
            end = dateArray.length;
        }
        int length = end - start;
        long result[][] = new long[arrays][];
        result[0] = new long[length];
        System.arraycopy(dateArray, start, result[0], 0, length);
        for (int i = 0; i < length; ++i) {
            result[0][i] *= 1000L; // convert back to milliseconds
        }
        for (int i = 1; i < arrays; ++i) {
            long array[] = readArrayFromAssets(context, baseName + "_" + i + ".bin");
            result[i] = new long[length];
            System.arraycopy(array, start, result[i], 0, length);
        }
        return result;
    }

}
