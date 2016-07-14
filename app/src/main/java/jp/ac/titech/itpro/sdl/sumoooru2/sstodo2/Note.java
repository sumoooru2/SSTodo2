package jp.ac.titech.itpro.sdl.sumoooru2.sstodo2;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class Note {
    public String tag, date, nid;
    private boolean expanded = false, editing = false;
    public boolean reDraw = true, visible = true, isTag = false;
    public int height = 5;
    private ArrayList<String> texts = new ArrayList<>();

    public void toggle() {
        expanded = !expanded;
        reDraw = true;
    }

    public void setEditing(boolean editState) {
        editing = editState;
        reDraw = true;
    }

    public boolean isEditing() {
        return editing;
    }

    public String makeText() {
        return makeText(editing || expanded ? -1 : height);
    }

    public String makeText(int lines) {
        if (texts == null) {
            return "";
        }
        String ret = "";
        int cnt = 0;
        for (String t : texts) {
            ret += t + '\n';
            cnt++;
            if (cnt == lines) {
                ret += ".....";
                break;
            }
        }
        return ret;
    }

    public void changeTexts(String text) {
        texts = new ArrayList<>(Arrays.asList(text.split("\n")));
    }

    public void changeLimit(String limit) {
        date = limit;
    }

    public String setFlags(String prevTag) {
        if (tag.equals("__end") || tag.equals("__begin")) {
            visible = false;
        } else if (!prevTag.equals(tag)) {
            isTag = true;
        } else if (texts.get(0).equals("")) {
            visible = false;
        }
        return tag;
    }

    public void write(JsonWriter writer) throws IOException {
        if (texts == null) {
            return;
        }
        writer.beginObject();
        writer.name("date").value(date);
        writer.name("texts");
        writer.beginArray();
        for (String s : texts) {
            writer.value(s);
        }
        writer.endArray();
        writer.name("tag").value(tag);
        writer.name("nid").value(nid);
        writer.endObject();
    }

    public void read(JsonReader reader) throws IOException {
        switch (reader.nextName()) {
            case "tag":
                tag = reader.nextString();
                break;
            case "date":
                date = reader.nextString();
                break;
            case "nid":
                nid = reader.nextString();
                break;
            case "texts":
                reader.beginArray();
                while (reader.hasNext()) {
                    texts.add(reader.nextString());
                }
                reader.endArray();
                break;
            default:
                Log.d("reader", "???");
                break;
        }

    }
}