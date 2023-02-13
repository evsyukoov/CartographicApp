package ru.evsyukoov.transform.convert;

public class Point {
    public String  name;
    public double  x;
    public double  y;
    public double  h;

    public Point(String name, double x, double y, double h) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.h = h;
    }

    public Point(double x, double y, double h) {
        this.x = x;
        this.y = y;
        this.h = h;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    @Override
    public String toString() {
        return "Point{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", h=" + h +
                '}';
    }
}
