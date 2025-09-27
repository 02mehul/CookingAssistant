package com.example.cookingassistant.ui;

import android.content.Context;
import android.net.Uri;

import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvUtils {

    public static void exportPantry(Context ctx, Uri dest) throws IOException {
        ItemDao dao = AppDatabase.getInstance(ctx).itemDao();
        List<Item> all = dao.getAllSync();
        try (OutputStream os = ctx.getContentResolver().openOutputStream(dest);
             OutputStreamWriter ow = new OutputStreamWriter(os, StandardCharsets.UTF_8);
             BufferedWriter w = new BufferedWriter(ow)) {

            w.write("name,quantity,category,expiryEpoch\n");
            if (all != null) {
                for (Item it : all) {
                    String name = quote(it.name);
                    String cat = quote(it.category);
                    String exp = (it.expiryEpoch == null ? "" : String.valueOf(it.expiryEpoch));
                    w.write(name + "," + it.quantity + "," + cat + "," + exp + "\n");
                }
            }
        }
    }

    public static void importPantry(Context ctx, Uri src) throws IOException {
        ItemDao dao = AppDatabase.getInstance(ctx).itemDao();
        try (InputStream is = ctx.getContentResolver().openInputStream(src);
             InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader r = new BufferedReader(ir)) {

            String line;
            boolean headerSkipped = false;
            while ((line = r.readLine()) != null) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                List<String> cols = parseCsv(line);
                if (cols.isEmpty()) continue;
                String name = unquote(get(cols,0)).trim();
                if (name.isEmpty()) continue;

                int qty = parseIntSafe(get(cols,1), 1);
                String cat = unquote(get(cols,2));
                Long exp = parseLongNullable(get(cols,3));

                Item existing = dao.findByName(name);
                if (existing == null) {
                    Item add = new Item(name, Math.max(1, qty));
                    add.name = name;
                    add.quantity = Math.max(1, qty);
                    add.category = (cat == null || cat.isEmpty()) ? null : cat;
                    add.expiryEpoch = exp;
                    dao.insert(add);
                } else {
                    existing.quantity = Math.max(1, existing.quantity + Math.max(0, qty));
                    if (existing.category == null && cat != null && !cat.isEmpty()) existing.category = cat;
                    if (existing.expiryEpoch == null && exp != null) existing.expiryEpoch = exp;
                    dao.update(existing);
                }
            }
        }
    }

    // ---- tiny CSV helpers ----
    private static String quote(String s) {
        if (s == null) return "";
        String q = s.replace("\"","\"\"");
        return "\"" + q + "\"";
    }
    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length()-1).replace("\"\"", "\"");
        }
        return s;
    }
    private static List<String> parseCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        if (line == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i=0;i<line.length();i++) {
            char c = line.charAt(i);
            if (c=='"') {
                if (inQ && i+1<line.length() && line.charAt(i+1)=='"') { cur.append('"'); i++; }
                else inQ = !inQ;
            } else if (c==',' && !inQ) {
                out.add(cur.toString()); cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
    private static String get(List<String> l, int i) { return i<l.size()? l.get(i): ""; }
    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch(Exception e) { return def; }
    }
    private static Long parseLongNullable(String s) {
        try { String t=s.trim(); return t.isEmpty()? null: Long.parseLong(t); } catch(Exception e){ return null; }
    }
}
