package org.mvel;

public class ErrorDetail {
    private int row;
    private int col;
    private boolean critical;
    private String message;

    public ErrorDetail(String message, boolean critical) {
        this.message = message;
        this.critical = critical;
    }

    public ErrorDetail(int row, int col, boolean critical, String message) {
        this.row = row;
        this.col = col;
        this.critical = critical;
        this.message = message;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String toString() {
         if (critical) {
             return "(" + row + "," + col + ") " + message;
         }
         else {
             return "(" + row + "," + col + ") WARNING: " + message;
         }

    }
}
