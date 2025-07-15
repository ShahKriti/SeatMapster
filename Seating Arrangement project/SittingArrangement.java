import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SittingArrangement {

    private List<List<String[][]>> building;
    private int totalClasses;

    // Constructor to initialize multiple floors and classes
    public SittingArrangement(int floors, int rows, int columns, int totalClasses) {
        this.totalClasses = totalClasses;
        building = new ArrayList<>();
        for (int i = 0; i < floors; i++) {
            List<String[][]> floor = new ArrayList<>();
            for (int j = 0; j < totalClasses; j++) { // Use totalClasses here
                String[][] seats = new String[rows][columns];
                for (int x = 0; x < rows; x++) {
                    for (int y = 0; y < columns; y++) {
                        seats[x][y] = "Empty";
                    }
                }
                floor.add(seats);
            }
            building.add(floor);
        }
    }

    // Method to get the seating HTML for a specific floor and class
    public synchronized String getSeatingHTML(int floor, int classIndex) {
        StringBuilder html = new StringBuilder("<table class='seating-table'>");
        String[][] seats = building.get(floor).get(classIndex);

        for (int i = 0; i < seats.length; i++) {
            html.append("<tr>");
            for (int j = 0; j < seats[i].length; j++) {
                String seatClass = seats[i][j].equals("Empty") ? "Empty" : "occupied";
                html.append("<td class='" + seatClass + "' onclick='assignSeat(" + floor + "," + classIndex + "," + i + "," + j + ", prompt(\"Enter name:\"))'>")
                        .append(seats[i][j])
                        .append("</td>");
                // Add Clear Seat button for each seat
                html.append("<td><form action='/clear' method='POST'>" +
                        "<input type='hidden' name='floor' value='" + floor + "' />" +
                        "<input type='hidden' name='class' value='" + classIndex + "' />" +
                        "<input type='hidden' name='row' value='" + i + "' />" +
                        "<input type='hidden' name='column' value='" + j + "' />" +
                        "<input type='submit' value='Clear Seat' />" +
                        "</form></td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    // Assign a seat
    public synchronized String assignSeat(int floor, int classIndex, int row, int column, String name) {
        String[][] seats = building.get(floor).get(classIndex);
        if (seats[row][column].equals("Empty")) {
            seats[row][column] = name;
            return "Seat assigned successfully to " + name + ".";
        } else {
            return "Seat is already occupied.";
        }
    }

    // Clear a seat
    public synchronized String clearSeat(int floor, int classIndex, int row, int column) {
        String[][] seats = building.get(floor).get(classIndex);
        if (!seats[row][column].equals("Empty")) {
            seats[row][column] = "Empty";
            return "Seat cleared successfully.";
        } else {
            return "Seat is already empty.";
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of floors: ");
        int floors = scanner.nextInt();
        System.out.print("Enter the number of rows: ");
        int rows = scanner.nextInt();
        System.out.print("Enter the number of columns: ");
        int columns = scanner.nextInt();
        System.out.print("Enter the number of classes per floor: ");
        int totalClasses = scanner.nextInt(); // Added option for number of classes per floor

        SittingArrangement arrangement = new SittingArrangement(floors, rows, columns, totalClasses);

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve static CSS and JS files
        server.createContext("/css", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String css = "body { font-family: Arial, sans-serif; background-color: #f4f4f4; color: #333; margin: 0; padding: 0; }" +
                        "h1 { text-align: center; margin-top: 20px; font-size: 2.5em; }" +
                        "h2 { margin: 20px 0; text-align: center; }" +
                        "table.seating-table { width: 80%; margin: 20px auto; border-collapse: collapse; text-align: center; }" +
                        "table.seating-table td { width: 50px; height: 50px; border: 1px solid #ddd; cursor: pointer; transition: background-color 0.3s ease; }" +
                        "table.seating-table td.Empty { background-color: #e0e0e0; }" +
                        "table.seating-table td.occupied { background-color: #ffcccb; color: #fff; }" +
                        "form { width: 80%; margin: 0 auto; padding: 20px; background-color: #fff; border-radius: 5px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); }" +
                        "form input[type='number'], form input[type='text'] { padding: 10px; margin: 5px 0; width: 100%; font-size: 1em; }" +
                        "form input[type='submit'] { padding: 10px 20px; font-size: 1.2em; background-color: #4CAF50; color: white; border: none; cursor: pointer; }" +
                        "form input[type='submit']:hover { background-color: #45a049; }" +
                        "button { padding: 5px 10px; background-color: #f44336; color: white; border: none; cursor: pointer; font-size: 0.9em; }" +
                        "button:hover { background-color: #d32f2f; }";

                exchange.getResponseHeaders().set("Content-Type", "text/css");
                exchange.sendResponseHeaders(200, css.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(css.getBytes());
                os.close();
            }
        });

        // Handle main page and seat assignment
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String method = exchange.getRequestMethod();
                String response = "";

                if (method.equalsIgnoreCase("GET")) {
                    // Get floor and class limits from the SittingArrangement instance
                    StringBuilder seatingLayout = new StringBuilder();

                    // Loop through each floor and class
                    for (int floor = 0; floor < floors; floor++) {
                        seatingLayout.append("<h2>Floor " + (floor + 1) + "</h2>");
                        for (int classIndex = 0; classIndex < totalClasses; classIndex++) { // Use totalClasses here
                            seatingLayout.append("<h3>Class " + (classIndex + 1) + "</h3>");
                            seatingLayout.append(arrangement.getSeatingHTML(floor, classIndex));  // Generate HTML for the seating
                        }
                    }

                    // Build the HTML response
                    response = "<html>" +
                            "<head><title>Sitting Arrangement</title><link rel='stylesheet' type='text/css' href='/css/style.css'></head>" +
                            "<body>" +
                            "<h1>Sitting Arrangement System</h1>" +
                            "<form method='POST'>" +
                            "Floor: <input type='number' name='floor' min='1' max='" + floors + "' required><br><br>" +
                            "Class: <input type='number' name='class' min='1' max='" + totalClasses + "' required><br><br>" +
                            "Row: <input type='number' name='row' required><br><br>" +
                            "Column: <input type='number' name='column' required><br><br>" +
                            "Name: <input type='text' name='name' required><br><br>" +
                            "<input type='submit' value='Assign Seat'>" +
                            "</form>" +
                            "<h2>Current Seating Arrangement</h2>" +
                            seatingLayout.toString() +
                            "</body></html>";
                } else if (method.equalsIgnoreCase("POST")) {
                    InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                    BufferedReader br = new BufferedReader(reader);
                    String query = br.readLine();
                    String[] params = query.split("&");
                    String floor = params[0].split("=")[1];
                    String classIndex = params[1].split("=")[1];
                    String row = params[2].split("=")[1];
                    String column = params[3].split("=")[1];
                    String name = params[4].split("=")[1];

                    int floorInt = Integer.parseInt(floor) - 1;
                    int classIndexInt = Integer.parseInt(classIndex) - 1;
                    int rowInt = Integer.parseInt(row);
                    int columnInt = Integer.parseInt(column);

                    String result = arrangement.assignSeat(floorInt, classIndexInt, rowInt, columnInt, name);

                    // Update the seating layout after assigning a seat
                    response = "<html><body><h1>" + result + "</h1>" +
                            arrangement.getSeatingHTML(floorInt, classIndexInt) + "</body></html>";
                }

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        // Handle seat clearing via POST
        server.createContext("/clear", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
                BufferedReader br = new BufferedReader(reader);
                String query = br.readLine();
                String[] params = query.split("&");

                String floor = params[0].split("=")[1];
                String classIndex = params[1].split("=")[1];
                String row = params[2].split("=")[1];
                String column = params[3].split("=")[1];

                int floorInt = Integer.parseInt(floor);
                int classIndexInt = Integer.parseInt(classIndex);
                int rowInt = Integer.parseInt(row);
                int columnInt = Integer.parseInt(column);

                String result = arrangement.clearSeat(floorInt, classIndexInt, rowInt, columnInt);
                String response = "<html><body><h1>" + result + "</h1>" +
                        arrangement.getSeatingHTML(floorInt, classIndexInt) + "</body></html>";

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        server.start();
        System.out.println("Server started at http://localhost:8080/");
    }
}