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
    

    /*  Questa funzione andrà implementata poi
    public Coord(int diameter){     //costruttore specializzato per la mappa
        this.x = Math.abs(Main.RND.nextInt()) % (Simulation.getMap().getWidth() - diameter);
        this.y = Math.abs(Main.RND.nextInt()) % (Simulation.getMap().getHeight() - diameter); 
    }

    */


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
        return Math.sqrt(Math.pow((this.x - point.x), 2) + Math.pow((this.y - point.y), 2));
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
