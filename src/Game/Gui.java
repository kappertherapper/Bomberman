package Game;

import Server.Client;
import Server.Common;
import Server.Server;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static Game.DebugLogger.LOG_FILE_PATH;

public class Gui extends Application {

    public static final int size = 30;
    public static final int scene_height = size * 20 + 75;
    public static final int scene_width = size * 20 + 200;

    public static Image image_floor;
    public static Image image_wall;
    public static Image hero_right, hero_left, hero_up, hero_down;
    static Label mazeLabel = new Label("Waiting for players...");
    //region Debugging
    static TextArea debugTA = new TextArea();
    static boolean isShowingDebugLog = false;
    private static Label[][] fields;
    private static boolean canMove = false;
    private static Timeline timeline = new Timeline();
    private TextArea scoreList;
    private boolean isServerInstance;
    private boolean isDebugEnabled;

    // -------------------------------------------
    // | Maze: (0,0)              | Score: (1,0) |
    // |-----------------------------------------|
    // | boardGrid (0,1)          | scorelist    |
    // |                          | (1,1)        |
    // -------------------------------------------
    private Client client;
    private Scene scene;

    public static void setLabelText(String s) {
        Platform.runLater(() -> {
            mazeLabel.setText(s);
        });
    }

    public static void setCanMove(boolean canMove) {
        Gui.canMove = canMove;
    }

    public static void removePlayerOnScreen(PlayerPosition oldpos) {
        Platform.runLater(() -> {
            fields[oldpos.getX()][oldpos.getY()].setGraphic(new ImageView(image_floor));
        });
    }

    public static void placePlayerOnScreen(PlayerPosition newpos, String direction) {
        Platform.runLater(() -> {
            int newx = newpos.getX();
            int newy = newpos.getY();
            if (direction.equals("right")) {
                fields[newx][newy].setGraphic(new ImageView(hero_right));
            }
            ;
            if (direction.equals("left")) {
                fields[newx][newy].setGraphic(new ImageView(hero_left));
            }
            ;
            if (direction.equals("up")) {
                fields[newx][newy].setGraphic(new ImageView(hero_up));
            }
            ;
            if (direction.equals("down")) {
                fields[newx][newy].setGraphic(new ImageView(hero_down));
            }
            ;
        });
    }

    public static void movePlayerOnScreen(PlayerPosition oldpos, PlayerPosition newpos, String direction) {
        removePlayerOnScreen(oldpos);
        placePlayerOnScreen(newpos, direction);
    }

    public static void clearLogFile() {
        try {
            Files.write(Paths.get(LOG_FILE_PATH), new byte[0]);
        } catch (IOException e) {
            DebugLogger.log(e.getMessage());
        }
    }

    public static void toggleDebugGUI(Stage primaryStage, GridPane grid, TextArea debugTA) {
        if (isShowingDebugLog) {
            grid.getChildren().remove(debugTA);
            primaryStage.setWidth(primaryStage.getWidth() - 300);
            isShowingDebugLog = false;
        } else {
            grid.add(debugTA, 1, 1, 2, 1);
            primaryStage.setWidth(primaryStage.getWidth() + 300);
            isShowingDebugLog = true;
        }
    }

    @Override
    public void init() {
        Parameters parameters = getParameters();
        if (!parameters.getRaw().isEmpty()) {
            String[] args = parameters.getRaw().toArray(new String[0]);
            if (args.length >= 2) {
                isServerInstance = Boolean.parseBoolean(args[0]);
                isDebugEnabled = Boolean.parseBoolean(args[1]);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {

            //region GameField setup
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(0, 10, 0, 10));

            mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

            Text scoreLabel = new Text("Score:");
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

            Button btnStart = new Button("Start");
            grid.add(btnStart, 0, 2);
            btnStart.setOnAction(e -> startGame());
            btnStart.setVisible(isServerInstance);


            scoreList = new TextArea();

            GridPane boardGrid = new GridPane();

            image_wall = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/wall4.png")), size, size, false, false);
            image_floor = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/floor1.png")), size, size, false, false);

            hero_right = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/heroRight.png")), size, size, false, false);
            hero_left = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/heroLeft.png")), size, size, false, false);
            hero_up = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/heroUp.png")), size, size, false, false);
            hero_down = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/heroDown.png")), size, size, false, false);

            fields = new Label[20][20];
            for (int j = 0; j < 20; j++) {
                for (int i = 0; i < 20; i++) {
                    switch (Generel.board[j].charAt(i)) {
                        case 'w':
                            fields[i][j] = new Label("", new ImageView(image_wall));
                            break;
                        case ' ':
                            fields[i][j] = new Label("", new ImageView(image_floor));
                            break;
                        default:
                            throw new Exception("Illegal field value: " + Generel.board[j].charAt(i));
                    }
                    boardGrid.add(fields[i][j], i, j);
                }
            }
            scoreList.setEditable(false);

            grid.add(mazeLabel, 0, 0);
            grid.add(scoreLabel, 1, 0);
            grid.add(boardGrid, 0, 1);
            grid.add(scoreList, 1, 1);

            scene = new Scene(grid, scene_width, scene_height);
            primaryStage.setScene(scene);
            primaryStage.show();
            //endregion

			/*// Putting default players on screen
			for (int i=0;i<GameLogic.players.size();i++) {
			  fields[GameLogic.players.get(i).getXpos()][GameLogic.players.get(i).getYpos()].setGraphic(new ImageView(hero_up));
			}*/

            scoreList.setText(getScoreList());

            //region Client Setup
            if (isDebugEnabled) {
                if (!isServerInstance) DebugLogger.log("Running with Debugging Enabled");
                else DebugLogger.logServer("Running with Debugging Enabled");
                setupDebug(primaryStage, grid);
            }

            if (isServerInstance) {
                DebugLogger.logServer("Running Application as Server");
                mazeLabel.setText("SERVER INSTANCE");
                grid.setStyle("-fx-background-color: lightblue;");
            } else {
                DebugLogger.log("Starting Game Application...");
                client = new Client();

                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (canMove) {
                        switch (event.getCode()) {
                            //TODO: only "right" and "down" works
                            case UP:
                                client.sendMessage("MOVE" + " " + "up" + " " + App.username);
                                System.out.println("CLICKED UP");
                                break;
                            case DOWN:
                                client.sendMessage("MOVE" + " " + "down" + " " + App.username);
                                System.out.println("CLICKED DOWN");
                                break;
                            case LEFT:
                                client.sendMessage("MOVE" + " " + "left" + " " + App.username);
                                System.out.println("CLICKED LEFT");
                                break;
                            case RIGHT:
                                client.sendMessage("MOVE" + " " + "right" + " " + App.username);
                                System.out.println("CLICKED RIGHT");
                                break;
                            case ESCAPE:
                                System.exit(0);
                                break;
                            case ENTER:
                                break;

                            default:
                                break;
                        }
                    }
                });

                PlayerPosition playerPosition = GameLogic.getRandomFreePosition();
                client.sendMessage("JOIN" + " " + App.username + " " + playerPosition.x + " " + playerPosition.y);
            }

        } catch (Exception e) {
            DebugLogger.log(e.getMessage());
        }
    }

    public void startGame() {
        final int countdownTime = 5;

        for (int i = countdownTime; i >= 0; i--) {
            final int remainingSeconds = i;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(countdownTime - i),
                    event -> {
                        //Server.broadcast("Starting game in " + remainingSeconds + " seconds...");
                        mazeLabel.setText("Starting game in " + remainingSeconds + " seconds"); // Only on server

                        Server.broadcast("COUNTER" + " " + "Starting:" + remainingSeconds);

                        if (remainingSeconds == 0) {
                            Server.broadcast("START");
                            Server.broadcast("COUNTER" + " " + "Game-Started!");
                        }
                    }
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    public void updateScoreTable() {
        Platform.runLater(() -> {
            scoreList.setText(getScoreList());
        });
    }

    public String getScoreList() {
        StringBuffer b = new StringBuffer(100);
        for (Player p : GameLogic.players) {
            b.append(p + "\r\n");
        }
        return b.toString();
    }

    public void setupDebug(Stage primaryStage, GridPane grid) {
        Button button = new Button("Debug");
        grid.add(button, 1, 2);

        debugTA.setEditable(false);
        debugTA.clear();
        debugTA.setPrefHeight(primaryStage.getHeight());
        debugTA.setPrefWidth(300);

        button.setOnAction(e -> toggleDebugGUI(primaryStage, grid, debugTA));

        Thread logReaderThread = new Thread(this::readLogFile);
        logReaderThread.setDaemon(true); // So that the thread stops when the application is closed
        logReaderThread.start();
    }

    private void readLogFile() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            long lastFileSize = 0;

            while (true) {
                long fileSize = logFile.length();

                if (fileSize > lastFileSize) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        // Skip to the end of the file
                        reader.skip(lastFileSize);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Append new log lines to the TextArea
                            String finalLine = line;
                            Platform.runLater(() -> debugTA.appendText(finalLine + "\n"));
                        }
                        lastFileSize = fileSize;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Wait for 1 second before checking the file again
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            DebugLogger.log(e.getMessage());
        }
    }
    //endregion


}

