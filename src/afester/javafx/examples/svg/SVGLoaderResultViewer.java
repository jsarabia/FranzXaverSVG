package afester.javafx.examples.svg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import afester.javafx.svg.GradientPolicy;
import afester.javafx.svg.SVGLoader;
import afester.javafx.svg.test.BasicTests;

public class SVGLoaderResultViewer extends Application {

    private Group svgImage = new Group();
    private final HBox mainLayout = new HBox();
    private SVGLoader loader = new SVGLoader();
    private String currentFile;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("SVGLoader result viewer");
        

        ObservableList<String> ol = FXCollections.observableList(getTestFiles());
        ListView<String> listView = new ListView<String>(ol);
        listView.getSelectionModel().getSelectedItems().addListener(
                new ListChangeListener<String>() {

                    @Override
                    public void onChanged(
                            javafx.collections.ListChangeListener.Change<? extends String> c) {
                        selectFile(c.getList().get(0));
                    }
                }
                );

/************** Options panel **************/
        HBox gradientPolicy = new HBox();
        gradientPolicy.setSpacing(10);
        gradientPolicy.getChildren().add(new Label("Gradient Transformation Policy:"));
        
        ComboBox<GradientPolicy> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(GradientPolicy.values());
        comboBox.setOnAction(e -> {
                loader.setGradientTransformPolicy(comboBox.getSelectionModel().getSelectedItem());
                selectFile(currentFile);
            }); 
        gradientPolicy.getChildren().add(comboBox);
        comboBox.getSelectionModel().select(GradientPolicy.USE_SUPPORTED);

        CheckBox showViewport = new CheckBox("Show Viewport");
        showViewport.setOnAction(e -> {
                loader.setAddViewboxRect(showViewport.isSelected());
                selectFile(currentFile);
            } );

        VBox controlPanel = new VBox();
        controlPanel.setPadding(new Insets(10, 10, 10, 10));
        controlPanel.getChildren().add(gradientPolicy);
        controlPanel.getChildren().add(showViewport);

        VBox leftPanel = new VBox();
        leftPanel.getChildren().add(listView);
        leftPanel.getChildren().add(controlPanel);
/************** Options panel **************/

        mainLayout.getChildren().add(leftPanel);
        mainLayout.getChildren().add(svgImage);

        listView.getSelectionModel().select(0);

        // show the generated scene graph
        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void selectFile(String string) {
        currentFile = string;
        InputStream svgFile = BasicTests.class.getResourceAsStream(string);
        
        mainLayout.getChildren().remove(svgImage);
        svgImage = loader.loadSvg(svgFile);
        mainLayout.getChildren().add(svgImage);
    }

    private List<String> getTestFiles() {
        List<String> result = new ArrayList<>();

        try {
            Set<String> resultSet = new HashSet<>();
            Enumeration<URL> resources = BasicTests.class.getClassLoader().getResources("afester/javafx/svg/test");
            while(resources.hasMoreElements()) {
                URL resourceRoot = resources.nextElement();
                File directory = new File(resourceRoot.toURI());
                String[] files = directory.list();
                for (String file : files) {
                    if (file.endsWith(".svg")) {
                        resultSet.add(file);
                    }
                }
            }

            result.addAll(resultSet);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return result;
    }
}
