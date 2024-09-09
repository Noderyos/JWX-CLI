package dev.noderyos;

import java.util.ArrayList;
import java.util.List;

public class TableDisplay {
    private final List<List<String>> rows;
    private List<String> header;

    public TableDisplay() {
        this.rows = new ArrayList<>();
        this.header = new ArrayList<>();
    }

    public TableDisplay(List<String> header) {
        this.rows = new ArrayList<>();
        this.header = header;
    }

    public void setHeader(List<String> header) {
        this.header = header;
    }

    public boolean addRow(List<String> row) {
        if(row.size() != this.header.size())
            return false;

        this.rows.add(row);
        return true;
    }

    public void removeRow(List<String> row) {
        this.rows.remove(row);
    }

    public void removeRow(int index) {
        if(index < 0)
            index = rows.size() + index;
        this.rows.remove(index);
    }

    public void display() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        List<Integer> widths = new ArrayList<>();
        for (String s : header)
            widths.add(s.length()+1);

        for(List<String> row : this.rows) {
            for (int i = 0; i < row.size(); i++) {
                if(row.get(i).length()+1 > widths.get(i))
                    widths.set(i, row.get(i).length()+1);
            }
        }

        for (int width : widths) {
            System.out.print("+" + repeat("-", width));
        }
        System.out.println("+");

        for (int i = 0; i < header.size(); i++) {
            System.out.print("|" +
                    padRight(header.get(i), widths.get(i)));
        }
        System.out.println("|");
        for (int width : widths) {
            System.out.print("+" + repeat("-", width));
        }
        System.out.println("+");

        for (List<String> row : this.rows) {
            for (int i = 0; i < row.size(); i++) {
                System.out.print("|" +
                        padRight(row.get(i), widths.get(i)));
            }
            System.out.println("|");
        }
        for (int width : widths) {
            System.out.print("+" + repeat("-", width));
        }
        System.out.println("+");
    }

    private String repeat(String with, int count) {
        return new String(new char[count]).replace("\0", with);
    }
    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}