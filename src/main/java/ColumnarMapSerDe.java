import static org.apache.hadoop.hive.serde.serdeConstants.LIST_COLUMNS;
import static org.apache.hadoop.hive.serde.serdeConstants.LIST_COLUMN_TYPES;
import static org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category.PRIMITIVE;
import static org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory.STRING;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class ColumnarMapSerDe implements SerDe {

  private List<String> columnNames;
  private ObjectInspector objectInspector;

  private Map<String, String> rowMap;
  private List<String> rowFields;

  private long deserializedByteCount;
  private SerDeStats stats;

  @Override
  public void initialize(Configuration conf, Properties tableProperties) throws SerDeException {
    final List<TypeInfo> columnTypes =
      TypeInfoUtils.getTypeInfosFromTypeString(tableProperties.getProperty(LIST_COLUMN_TYPES));
    for (TypeInfo type : columnTypes) {
      if (!type.getCategory().equals(PRIMITIVE) || !((PrimitiveTypeInfo) type).getPrimitiveCategory().equals(STRING)) {
        throw new SerDeException("This serde only supports primitive types.");
      }
    }

    columnNames = Arrays.asList(tableProperties.getProperty(LIST_COLUMNS).split(","));
    List<ObjectInspector> columnObjectInspectors =
      Collections.nCopies(
        columnNames.size(),
        (ObjectInspector) PrimitiveObjectInspectorFactory.javaStringObjectInspector);
    objectInspector = ObjectInspectorFactory.getStandardStructObjectInspector(columnNames, columnObjectInspectors);

    rowMap = new HashMap<String, String>(columnNames.size());
    rowFields = new ArrayList<String>(columnNames.size());

    stats = new SerDeStats();
    deserializedByteCount = 0;
  }

  @Override
  public Writable serialize(Object obj, ObjectInspector objectInspector) throws SerDeException {
    StringBuilder builder = new StringBuilder();

    StructObjectInspector structOI = (StructObjectInspector) objectInspector;
    List<? extends StructField> structFields = structOI.getAllStructFieldRefs();

    if (structFields.size() != columnNames.size()) {
      throw new SerDeException("Cannot serialize this data: number of input fields must be " + columnNames.size());
    }

    for (int i = 0; i < structFields.size(); i++) {
      StructField structField = structFields.get(i);
      Object fieldData = structOI.getStructFieldData(obj, structField);
      StringObjectInspector fieldOI = (StringObjectInspector) structField.getFieldObjectInspector();
      String fieldContent = fieldOI.getPrimitiveJavaObject(fieldData);
      if (fieldContent != null) {
        String fieldName = columnNames.get(i);
        if (builder.length() > 0) {
          builder.append("\001");
        }
        builder.append(fieldName).append("\002").append(fieldContent);
      }
    }
    return new Text(builder.toString());
  }

  @Override
  public Object deserialize(Writable writable) throws SerDeException {
    Text text = (Text) writable;

    rowMap.clear();
    rowFields.clear();

    String date;
    String timeStamp;
    String entryType;
    String guid;
    String username;
    String servicearchive;
    String logger ="";
    String procthread ="";
    String tag;
    String duration ="";
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

    int length = (bsplit[1].split("\\s")).length;
    if ( length > 2) {
      duration = bsplit[1].split("\\s")[2];
      if (duration.contains("after")) {
        duration = bsplit[1].split("\\s")[3];
      } else duration = "";
    }

    if (tag.equals("") && duration.equals("")){
      info = content.substring(content.indexOf(bsplit[1]), content.length());
    }else if (tag.equals("") && length >2) {
      info = content.substring(content.indexOf(bsplit[1].split("\\s")[2]), content.length());
    }else if (tag.equals("") && length < 2) {
      info = content.substring(content.indexOf(bsplit[1].split("\\s")[1]), content.length());
    }
    else if (duration.equals("")) {
      info = content.substring(content.indexOf(bsplit[1].split("\\s")[3]), content.length());
    }else{
      info = content.substring(content.indexOf(bsplit[1].split("\\s")[4]), content.length());
    }

    rowFields.add(date);
    rowFields.add(timeStamp);
    rowFields.add(entryType);
    rowFields.add(guid);
    rowFields.add(username);
    rowFields.add(servicearchive);
    rowFields.add(logger);
    rowFields.add(procthread);
    rowFields.add(tag);
    rowFields.add(duration);
    rowFields.add(info);

    return rowFields;
  }

  @Override
  public ObjectInspector getObjectInspector() throws SerDeException {
    return objectInspector;
  }

  @Override
  public Class<? extends Writable> getSerializedClass() {
    return Text.class;
  }

  @Override
  public SerDeStats getSerDeStats() {
    stats.setRawDataSize(deserializedByteCount);
    return stats;
  }

}
