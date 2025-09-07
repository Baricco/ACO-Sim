package com.example.graphics;


public class Coord {
    
    public double x;
    public double y;
    
    public Coord(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Coord(Coord coord){
        this.x = coord.x;
        this.y = coord.y;
    }


    public void multiply(double factor) {
        this.x *= factor;
        this.y *= factor;
    }

    public void normalize() {
        double length = Math.sqrt(x * x + y * y);
        if (length != 0) {
            this.x = x / length;
            this.y = y / length;
        }
    }

    public double distance(Coord point){
        return Math.sqrt(distanceSquared(point));
    }

    public double distanceSquared(Coord point) {
        double dx = this.x - point.x;
        double dy = this.y - point.y;
        return dx * dx + dy * dy;
    }

    
    public void sum(Coord coord) {
        this.x += coord.x;
        this.y += coord.y;
    }

    public void subtract(Coord coord) {
        this.x -= coord.x;
        this.y -= coord.y;
    }

    public Coord copy() {
        return new Coord(this.x, this.y);
    }

    @Override
    public String toString() {
        return "{" + x + ", " + y + "}";
    }


    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double dot(Coord direction) {
        return (this.x * direction.x + this.y * direction.y);
    }

}
