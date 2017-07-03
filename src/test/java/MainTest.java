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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pawelwiejkut on 02.07.2017.
 */
@SuppressWarnings("ALL")
public class MainTest {





    public static void deserialize (Text text)  {

        String date;
        String timeStamp;
        String entryType;
        String guid;
        String username;
        String servicearchive;
        String logger ="";
        String procthread ="";
        String tag;
        String duration;
        String info;

        String content = text.toString();
        String[] space = content.split("\\s");

        date = space[0];
        timeStamp = space[1];
        entryType = space[2];
        guid = space[3];
        username = space[4];
        servicearchive = space[5];

        Matcher m2 = Pattern.compile("\\[(.*?)\\]").matcher(content);
         if (m2.find()) {
          logger = m2.group(1);
          System.out.println(m2.group(1));
        }

        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(content);
        if (m.find()) {
            procthread = m.group(1);
            System.out.println( m.group(1));
        }

        String[] bsplit = content.split("\\)");
        tag = bsplit[1].split("\\s")[1];
        if (tag.equals("ENTER") || tag.equals("EXIT")){}
        else tag = "";

        duration = bsplit[1].split("\\s")[2];
        if (duration.contains("after")){
            duration = bsplit[1].split("\\s")[3];
        }
        else duration = "";

        if (tag.equals("") && duration.equals("")){
            info = content.substring(content.indexOf(bsplit[1].split("\\s")[1]), content.length());
        }else if (tag.equals("")) {
            info = content.substring(content.indexOf(bsplit[1].split("\\s")[2]), content.length());
        }
        else if (duration.equals("")) {
            info = content.substring(content.indexOf(bsplit[1].split("\\s")[3]), content.length());
        }else{
            info = content.substring(content.indexOf(bsplit[1].split("\\s")[4]), content.length());
        }

    }


    public static void main(String[] args) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("sources/ula"));
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
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
