
package grayscale;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author juju
 */
public class Grayscale extends Application {
    ArrayList<File> files = new ArrayList<>();    
    ProgressIndicator pb = new ProgressIndicator(0.6);
    Alert alert; 
    int prog;    
    CheckBox check=new CheckBox("Replace original files");
    Label info = new Label();
    ObservableList<String> items =FXCollections.observableArrayList ();
 
public class Progress extends Thread{       
    
    @Override
    public void run(){
                ProcessBuilder p;   
        Process proc;        
        //String noext,ext;
        ArrayList<File> omit = new ArrayList<>();
        boolean error=false;
        if(files.isEmpty()){
            Platform.runLater(() -> {
                alert=new Alert(AlertType.WARNING);
                alert.setResizable(true);
                alert.setHeaderText("No files !");
                alert.setContentText("You have not given any files to convert !");
                alert.showAndWait();
            });
            return;
        }
        Platform.runLater(() -> pb.setVisible(true));
        Platform.runLater(() -> info.setText("Identifying..."));
        try {           
            for(prog=0;prog<files.size() && !error;prog++){
                double d=(((prog+1)/2.0)/(double)files.size());  
                System.out.println("i "+d);
                Platform.runLater(() -> pb.setProgress(d));
                if("Linux".equals(System.getProperty("os.name")))
                    p=new ProcessBuilder("identify",files.get(prog).getAbsolutePath());
                else if (System.getProperty("os.name").contains("Windows"))
                    p=new ProcessBuilder("magick","identify",files.get(prog).getAbsolutePath());
                else
                    p=new ProcessBuilder("/opt/local/bin/identify",files.get(prog).getAbsolutePath());                
                proc=p.start();
                proc.waitFor();
                if(proc.exitValue()!=0){                    
                    int i=prog;
                    final FutureTask query = new FutureTask(() -> {
                        alert= new Alert(AlertType.WARNING);
                        alert.setHeaderText("Not an image file !");
                        alert.setContentText("The file "+files.get(i).getAbsolutePath()+" is not an image file or is corrupted !");
                        alert.setResizable(true);
                        alert.showAndWait();  
                        return null;
                    });                   
                    Platform.runLater(query);
                    try {
                        query.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(Grayscale.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    omit.add(files.get(i));
                }
            }                        
            for(prog=0;prog<files.size() && !error;prog++){                                                       
                        double d=(((prog+1)/2.0)/(double)files.size())+0.5;
                        System.out.println(d);
                        Platform.runLater(() -> pb.setProgress(d));
                        //System.out.println(!check.isSelected());
                        if(!omit.contains(files.get(prog))){
                            if(!check.isSelected()){
                                /*
                                if(files.get(prog).getAbsolutePath().contains(".")){
                                    noext=files.get(prog).getAbsolutePath().substring(0, files.get(prog).getAbsolutePath().lastIndexOf('.'));
                                    ext=files.get(prog).getAbsolutePath().substring(files.get(prog).getAbsolutePath().lastIndexOf('.'), files.get(prog).getAbsolutePath().length());
                                }
                                else{
                                    noext=files.get(prog).getAbsolutePath();
                                    ext="";
                                }
                                */
                                String natdir=files.get(prog).getAbsoluteFile().toString();
                                String path=natdir.substring(0,natdir.lastIndexOf(File.separator));
                                if("Linux".equals(System.getProperty("os.name")))
                                    p=new ProcessBuilder("convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",path+"/Gray-"+files.get(prog).toPath().getFileName());
                                else if(System.getProperty("os.name").contains("Windows"))
                                    p=new ProcessBuilder("magick","convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",path+"\\Gray-"+files.get(prog).toPath().getFileName());
                                else
                                    p=new ProcessBuilder("/opt/local/bin/convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",path+"/Gray-"+files.get(prog).toPath().getFileName());
                            }
                            else{                            
                                if("Linux".equals(System.getProperty("os.name")))
                                    p=new ProcessBuilder("convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",files.get(prog).getAbsolutePath());
                                else if(System.getProperty("os.name").contains("Windows"))
                                    p=new ProcessBuilder("magick","convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",files.get(prog).getAbsolutePath());
                                else
                                    p=new ProcessBuilder("/opt/local/bin/convert",files.get(prog).getAbsolutePath(),"-colorspace","gray",files.get(prog).getAbsolutePath());
                            }
                            proc=p.start();
                            proc.waitFor();                        
                            if(proc.exitValue()!=0){
                                error=true;
                                int i=prog;
                                int b;
                                while((b=proc.getErrorStream().read())!=-1)
                                    System.out.print((char)b);
                                String file=files.get(i).getAbsolutePath();
                                Platform.runLater(() -> {
                                    alert= new Alert(AlertType.ERROR);
                                    alert.setResizable(true);
                                    alert.setHeaderText("Error during conversion !");
                                    alert.setContentText("The file "+file+" could not be converted ! Check if the file is not an image file and/or is not corrupted. If this error still appears ImageMagick may not be able to convert this image.");
                                    alert.showAndWait();
                                });                                                
                            }
                        }
            }                            
        } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Grayscale.class.getName()).log(Level.SEVERE, null, ex);    
                    error=true;
                    Platform.runLater(() -> {
                        alert= new Alert(AlertType.ERROR);
                        alert.setResizable(true);
                        alert.setHeaderText("Exception thrown !");
                        alert.setContentText(ex.toString());
                        alert.showAndWait();
                    });
        }
        if(!error){
            Platform.runLater(() -> {
                alert=new Alert(AlertType.INFORMATION);
            });
            if(files.size()==1)
                Platform.runLater(() -> alert.setContentText("The file has been successfully converted !"));
            else if(files.size()>1)
                Platform.runLater(() -> alert.setContentText("The files have been successfully converted !"));                            
            Platform.runLater(() -> alert.showAndWait()); 
            Platform.runLater(() -> items.clear());
            files.clear();
        } 
        Platform.runLater(() -> info.setText(""));
    }
}

    public void addfilelist(File[] f){               
        for (File f1 : f) {
            if (!f1.isDirectory()) {
                items.add(f1.toString());
                files.add(f1);
            } else {
                addfilelist(f1.listFiles());
            }
        }                               
    }
    
    @Override
    public void start(Stage primaryStage) {             
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");            
        check.setSelected(true);
        ListView<String> list = new ListView<>();         
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);               
        list.setItems(items);
        ImageView l = new ImageView();
        l.setImage(new Image(getClass().getClassLoader().getResource("resources/Grayscale.png").toString()));        
        l.setFitWidth(800);
        l.setPreserveRatio(true);
        Button btn = new Button();        
        Button add = new Button();
        Button rm = new Button();        
        pb.setMinSize(50, 50);        
        pb.setMaxSize(50, 50);
        pb.setVisible(false);
        //info.setText("Identifying...");
        info.setText("");
        btn.setText("Apply Grayscale");
        btn.setOnAction((ActionEvent event) -> {
            boolean error=false;            
            if(!error)
                new Progress().start();                
        });
        add.setText("Add");
        add.setOnAction((ActionEvent event) -> {
            int start = files.size();
            List<File> L = fileChooser.showOpenMultipleDialog(new Stage());
            if(L!=null){
                for(int i=0;i<L.size();i++)
                    files.add(L.get(i));
                for(int i=start;i<files.size();i++)
                    items.add(files.get(i).toString());            
                pb.setVisible(false);
            }
        });
        rm.setText("Remove");
        rm.setOnAction((ActionEvent event) -> {
            Object[] item = list.getSelectionModel().getSelectedItems().toArray(); 
            boolean end;
            if(item.length!=0)                
                pb.setVisible(false);
            for (Object item1 : item) {
                end=false;
                items.remove(item1.toString());                
                for(int i=0;i<files.size() && !end;i++)
                    if(files.get(i).toString() == null ? item1.toString() == null : files.get(i).toString().equals(item1.toString())){
                        files.remove(i);                       
                        end=true;
                    }                
            }
            list.getSelectionModel().clearSelection();                        
        });
        
        list.setOnKeyReleased((KeyEvent k) -> {
            if(k.getCode()==KeyCode.DELETE || k.getCode()==KeyCode.BACK_SPACE)
                rm.fire();
        });
        
        list.setOnDragOver((DragEvent event) -> {           
            event.acceptTransferModes(TransferMode.ANY);            
            event.consume();            
        });
        
        list.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;            
            if (db.hasFiles()) {                
                List<File> f=db.getFiles();                 
                addfilelist(f.toArray(new File[0]));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        StackPane root = new StackPane();       
        
        StackPane.setMargin(list, new Insets(100,40,100,40));        
        StackPane.setAlignment(btn,Pos.BOTTOM_CENTER);        
        StackPane.setAlignment(add,Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(rm,Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(l,Pos.TOP_CENTER);
        StackPane.setAlignment(pb,Pos.BOTTOM_CENTER);
        StackPane.setAlignment(check, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(info, Pos.BOTTOM_CENTER);
        StackPane.setMargin(btn, new Insets(0,0,40,0));        
        StackPane.setMargin(rm, new Insets(0,60,40,0));
        StackPane.setMargin(add, new Insets(0,140,40,0));
        StackPane.setMargin(l, new Insets(25,0,0,0));
        StackPane.setMargin(pb, new Insets(0,0,20,250));        
        StackPane.setMargin(check, new Insets(0,0,40,25));
        StackPane.setMargin(info, new Insets(0,0,75,260));
        root.getChildren().add(btn);
        root.getChildren().add(add);        
        root.getChildren().add(rm);
        root.getChildren().add(list);
        root.getChildren().add(l);
        root.getChildren().add(pb);
        root.getChildren().add(check);
        root.getChildren().add(info);
                
        Scene scene = new Scene(root, 850, 600);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResource("resources/G.png").toString()));
        primaryStage.setTitle("Grayscale");
        primaryStage.setScene(scene);
        primaryStage.show();        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
