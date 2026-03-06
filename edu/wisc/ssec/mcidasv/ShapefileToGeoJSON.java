package edu.wisc.ssec.mcidasv;

import ucar.unidata.util.FileManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.logging.Logger;

public class ShapefileToGeoJSON {
    private static final Logger logger = Logger.getLogger(ShapefileToGeoJSON.class.getName());

    public static boolean convert(String inputPath, String outputPath) {
        String dbfPath = inputPath.substring(0, inputPath.lastIndexOf(".")) + ".dbf";
        File shpFile = new File(inputPath);
        File dbfFile = new File(dbfPath);

        if (!shpFile.exists()) return false;

        try (DataInputStream shpStream = new DataInputStream(new FileInputStream(shpFile));
             FileWriter writer = new FileWriter(outputPath)) {

            shpStream.skipBytes(100);

            List<Map<String, Object>> attributes = dbfFile.exists() ? parseDbf(dbfFile) : new ArrayList<>();

            writer.write("{\"type\": \"FeatureCollection\", \"features\": [");
            int recordIdx = 0;
            boolean firstFeature = true;

            while (shpStream.available() > 0) {
                int recordNum = shpStream.readInt();
                int length = shpStream.readInt() * 2;
                byte[] data = new byte[length];
                shpStream.readFully(data);
                ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

                int type = bb.getInt(0);
                if (type == 3 || type == 5) {
                    if (!firstFeature) writer.write(",");
                    firstFeature = false;

                    writer.write("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
                    int numPoints = bb.getInt(40);
                    int coordStart = 44 + (bb.getInt(36) * 4);

                    for (int i = 0; i < numPoints; i++) {
                        double lon = bb.getDouble(coordStart + (i * 16));
                        double lat = bb.getDouble(coordStart + (i * 16) + 8);
                        writer.write("[" + lon + "," + lat + "]");
                        if (i < numPoints - 1) writer.write(",");
                    }
                    writer.write("]}, \"properties\": ");

                    if (recordIdx < attributes.size()) {
                        writer.write(mapToJson(attributes.get(recordIdx)));
                    } else {
                        writer.write("{}");
                    }
                    writer.write("}");
                }
                recordIdx++;
            }
            writer.write("]}");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<Map<String, Object>> parseDbf(File file) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[32];
            fis.read(header);
            int numRecords = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
            short headerLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
            short recordLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getShort(10);

            List<String> fieldNames = new ArrayList<>();
            int bytesRead = 32;
            while (bytesRead < headerLength - 1) {
                byte[] fieldBuf = new byte[32];
                fis.read(fieldBuf);
                fieldNames.add(new String(fieldBuf, 0, 11).trim());
                bytesRead += 32;
            }
            fis.skip(1);

            for (int i = 0; i < numRecords; i++) {
                byte[] recBuf = new byte[recordLength];
                fis.read(recBuf);
                Map<String, Object> row = new HashMap<>();
                int offset = 1;
                for (String name : fieldNames) {
                    row.put(name, new String(recBuf, offset, 10).trim());
                    offset += 10;
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        return sb.append("}").toString();
    }

    public static String getShapeFile() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Shape Files (*.shp)", "shp");
        return FileManager.getReadFile("Load File", filter);
    }
}