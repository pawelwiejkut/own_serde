import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by pawelwiejkut on 02.07.2017.
 */
@SuppressWarnings("ALL")
public class MainTest {





    public static Object deserialize(Text text) throws SerDeException {

        List<String> columnNames = null;
        ObjectInspector objectInspector;

        Map<String, String> rowMap = null;
        List<String> rowFields = null;

        long deserializedByteCount = 0;
        SerDeStats stats;


        String content = text.toString();
        deserializedByteCount += text.getBytes().length;
        String[] pairs = content.split("\001");
        for (String pair : pairs) {
            int delimiterIndex = pair.indexOf('\002');
            if (delimiterIndex >= 0) {
                String key = pair.substring(0, delimiterIndex);
                String value = pair.substring(delimiterIndex + 1);
                rowMap.put(key, value);
            }
        }

//        for (String columnName : columnNames) {
//            rowFields.add(rowMap.get(columnName));
//        }
//
        return rowFields;
    }


    public static void main(String[] args) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("sources/server.log.2017-05-17-10"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
                Text text = new Text(line);
                deserialize(text);

            }
            String everything = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SerDeException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
