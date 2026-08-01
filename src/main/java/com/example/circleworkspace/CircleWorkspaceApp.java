package com.example.circleworkspace;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.*;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import static com.example.circleworkspace.Model.*;

public final class CircleWorkspaceApp extends Application {
    private final ObservableList<CircleState> circles = FXCollections.observableArrayList();
    private final ObservableList<ContactState> contacts = FXCollections.observableArrayList();
    private final RotationSolver solver = new RotationSolver();
    private final WorkspaceStore store = new WorkspaceStore();
    private final LongProperty tick = new SimpleLongProperty(0);
    private final DoubleProperty ticksPerSecond = new SimpleDoubleProperty(2);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final IntegerProperty selectedId = new SimpleIntegerProperty(-1);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1);
    private double panX=0, panY=0, dragOffsetX, dragOffsetY, panStartX, panStartY;
    private boolean draggingCircle, panning;
    private WorkspaceData starting;
    private Canvas canvas;
    private VBox inspector;
    private long lastNanos;

    @Override public void start(Stage stage) throws Exception {
        starting = store.loadResource("/workspace.json");
        applyData(starting);
        canvas = new Canvas(1100,800);
        var canvasPane = new StackPane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        inspector = new VBox(10);
        inspector.setPadding(new Insets(12));
        inspector.setPrefWidth(330);
        var scroll = new ScrollPane(inspector); scroll.setFitToWidth(true); scroll.setPrefWidth(350);
        var root = new BorderPane(canvasPane, topBar(stage), null, null, scroll);
        var scene = new Scene(root, 1450, 900);
        stage.setTitle("Variable Radius Circle Workspace — Java 25");
        stage.setScene(scene); stage.setMaximized(true); stage.show();
        installCanvasHandlers(scene);
        selectedId.addListener((o,a,b)->rebuildInspector());
        circles.addListener((ListChangeListener<CircleState>) c->{ redraw(); rebuildInspector(); });
        contacts.addListener((ListChangeListener<ContactState>) c->{ redraw(); rebuildInspector(); });
        tick.addListener(o->{redraw(); rebuildInspector();});
        rebuildInspector(); redraw();
        new AnimationTimer(){ @Override public void handle(long now){
            if(lastNanos==0) lastNanos=now;
            if(running.get() && now-lastNanos >= 1_000_000_000.0/ticksPerSecond.get()) { tick.set(tick.get()+1); lastNanos=now; }
        }}.start();
    }

    private Node topBar(Stage stage) {
        var bar = new HBox(8); bar.setPadding(new Insets(8)); bar.setAlignment(Pos.CENTER_LEFT);
        var run = new ToggleButton("Run"); run.selectedProperty().bindBidirectional(running); run.textProperty().bind(running.map(v->v?"Pause":"Run"));
        var tickField = longField(tick.get(), v->tick.set(v)); tickField.setPrefWidth(90); tickField.textProperty().bindBidirectional(tick, new javafx.util.converter.NumberStringConverter());
        var speed = doubleField(ticksPerSecond.get(), v->ticksPerSecond.set(Math.max(.05,v))); speed.setPrefWidth(80);
        var add = new Button("Add"); add.setOnAction(e->addCircle());
        var dup = new Button("Duplicate"); dup.setOnAction(e->duplicateSelected());
        var del = new Button("Delete"); del.setOnAction(e->deleteSelected());
        var reset = new Button("Reset start"); reset.setOnAction(e->applyData(starting));
        var save = new Button("Save…"); save.setOnAction(e->save(stage));
        var load = new Button("Load…"); load.setOnAction(e->load(stage));
        var fit = new Button("Fit all"); fit.setOnAction(e->fitAll());
        bar.getChildren().addAll(run,new Label("Tick"),tickField,new Label("ticks/sec"),speed,new Separator(),add,dup,del,fit,reset,save,load);
        return bar;
    }

    private void rebuildInspector() {
        if(inspector==null) return; inspector.getChildren().clear();
        var c = selected();
        inspector.getChildren().add(new Label(c==null?"No circle selected":"Circle "+c.id()));
        if(c==null) return;
        var results=solver.solve(circles,contacts); var rr=results.get(c.id());
        addMetric("Radius", c.radius(), v->replace(c.withRadius(Math.max(.1,v))));
        addMetric("Diameter", c.diameter(), v->replace(c.withRadius(Math.max(.1,v/2))));
        addMetric("Circumference", c.circumference(), v->replace(c.withRadius(Math.max(.1,v/Math.TAU))));
        double angle = normalize(c.startAngleDeg()+rr.rateDegPerTick()*tick.get());
        addMetric("Rotation degrees", angle, v->{ replace(c.withStartAngle(normalize(v-rr.rateDegPerTick()*tick.get()))); validateContacts(); });
        addMetric("Rotation units", angle/360*c.circumference(), v->{ double a=v/c.circumference()*360; replace(c.withStartAngle(normalize(a-rr.rateDegPerTick()*tick.get()))); validateContacts(); });
        var powered = new CheckBox("Own rotation enabled"); powered.setSelected(c.powered()); powered.setOnAction(e->replace(c.withPower(powered.isSelected()))); inspector.getChildren().add(powered);
        addMetric("Own rate °/tick", c.ownRateDegPerTick(), v->replace(c.withRate(v)), !c.powered());
        addMetric("Own rate units/tick", c.ownRateDegPerTick()/360*c.circumference(), v->replace(c.withRate(v/c.circumference()*360)), !c.powered());
        inspector.getChildren().add(new Label("Effective: "+fmt(rr.rateDegPerTick())+"°/tick — "+rr.mode()+" depth "+rr.depth()));
        inspector.getChildren().add(new Separator()); inspector.getChildren().add(new Label("Contacts"));
        for(var ct: contacts.stream().filter(x->x.aId()==c.id()||x.bId()==c.id()).toList()) inspector.getChildren().add(contactPane(c,ct));
    }

    private Node contactPane(CircleState c, ContactState ct) {
        int other=ct.other(c.id()); var box=new VBox(5); box.setPadding(new Insets(8)); box.setStyle("-fx-border-color: -fx-box-border; -fx-border-radius: 5;");
        box.getChildren().add(new Label("Contact "+ct.id()+" with circle "+other+" — "+ct.type()));
        addMetricTo(box,"Selected touch °",ct.touchFor(c.id()),v->{replaceContact(ct.withTouchFor(c.id(),normalize(v))); repositionFromContact(c.id(),ct.id());});
        addMetricTo(box,"Selected touch units",ct.touchFor(c.id())/360*c.circumference(),v->{replaceContact(ct.withTouchFor(c.id(),normalize(v/c.circumference()*360))); repositionFromContact(c.id(),ct.id());});
        var slave=new RadioButton("Use as rotation input"); slave.setSelected(Objects.equals(c.slaveContactId(),ct.id())); slave.setDisable(c.powered()); slave.setOnAction(e->replace(c.withSlave(slave.isSelected()?ct.id():null)));
        var br=new Button("Break contact"); br.setOnAction(e->breakContact(ct.id())); box.getChildren().addAll(slave,br); return box;
    }

    private void installCanvasHandlers(Scene scene) {
        canvas.setOnScroll(e->{ double old=zoom.get(); double factor=e.getDeltaY()>0?1.12:1/1.12; zoom.set(Math.clamp(old*factor,.1,8)); panX=e.getX()-(e.getX()-panX)*zoom.get()/old; panY=e.getY()-(e.getY()-panY)*zoom.get()/old; redraw(); });
        canvas.setOnMousePressed(e->{ canvas.requestFocus(); var world=world(e.getX(),e.getY()); var hit=hit(world[0],world[1]);
            if(e.getButton()==MouseButton.MIDDLE || e.getButton()==MouseButton.SECONDARY){panning=true;panStartX=e.getX()-panX;panStartY=e.getY()-panY;return;}
            if(hit!=null){selectedId.set(hit.id()); draggingCircle=true; dragOffsetX=world[0]-hit.x(); dragOffsetY=world[1]-hit.y(); replace(hit.withStartAngle(0)); contacts.removeIf(ct->ct.aId()==hit.id()||ct.bId()==hit.id());}
            else selectedId.set(-1);
        });
        canvas.setOnMouseDragged(e->{ if(panning){panX=e.getX()-panStartX;panY=e.getY()-panStartY;redraw();return;} if(draggingCircle){var c=selected();if(c!=null){var w=world(e.getX(),e.getY());replace(c.withPosition(w[0]-dragOffsetX,w[1]-dragOffsetY));}}});
        canvas.setOnMouseReleased(e->{if(draggingCircle){draggingCircle=false;snapSelected();}panning=false;});
        scene.setOnKeyPressed(e->{var c=selected(); if(c==null)return; double d=e.isShiftDown()?10:1; switch(e.getCode()){case LEFT->moveNoSnap(c,-d,0);case RIGHT->moveNoSnap(c,d,0);case UP->moveNoSnap(c,0,-d);case DOWN->moveNoSnap(c,0,d);case DELETE,BACK_SPACE->deleteSelected();default->{}}});
    }

    private void redraw() {
        if(canvas==null)return; var g=canvas.getGraphicsContext2D(); g.setFill(Color.web("#f7f8fb"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        g.save();g.translate(panX,panY);g.scale(zoom.get(),zoom.get()); var results=solver.solve(circles,contacts);
        for(var c:circles) drawCircle(g,c,results.get(c.id()));
        for(var ct:contacts) drawContact(g,ct,results); g.restore();
    }

    private void drawCircle(GraphicsContext g,CircleState c,RotationSolver.Result r){ double ang=normalize(c.startAngleDeg()+r.rateDegPerTick()*tick.get()); Color stroke=switch(r.mode()){case POWERED->Color.DODGERBLUE;case SLAVED->Color.hsb(125,Math.max(.25,.75-r.depth()*.08),.65);case STOPPED->Color.DARKGRAY;};
        g.setLineWidth(c.id()==selectedId.get()?4:2);g.setStroke(stroke);g.strokeOval(c.x()-c.radius(),c.y()-c.radius(),c.radius()*2,c.radius()*2);
        int marks=Math.max(4,(int)Math.round(c.circumference())); g.setLineWidth(1);g.setFill(Color.BLACK);g.setFont(javafx.scene.text.Font.font(Math.clamp(11/zoom.get(),4,14)));
        for(int i=0;i<marks;i++){double a=Math.toRadians(ang+i*360.0/marks);double ux=Math.sin(a),uy=-Math.cos(a);double x1=c.x()+ux*c.radius(),y1=c.y()+uy*c.radius(),x2=c.x()+ux*(c.radius()-Math.min(8,c.radius()*.15)),y2=c.y()+uy*(c.radius()-Math.min(8,c.radius()*.15));g.strokeLine(x1,y1,x2,y2);if(i%Math.max(1,marks/12)==0)g.fillText(Integer.toString(i),c.x()+ux*(c.radius()+10),c.y()+uy*(c.radius()+10));}
        g.fillText("#"+c.id(),c.x()-8,c.y()+4);
    }

    private void drawContact(GraphicsContext g,ContactState ct,Map<Integer,RotationSolver.Result> r){var a=find(ct.aId());var b=find(ct.bId());if(a==null||b==null)return;double x=(a.x()+b.x())/2,y=(a.y()+b.y())/2;boolean trans=(Objects.equals(a.slaveContactId(),ct.id())&&r.get(a.id()).mode()==DriveMode.SLAVED)||(Objects.equals(b.slaveContactId(),ct.id())&&r.get(b.id()).mode()==DriveMode.SLAVED);g.setFill(trans?Color.ORANGE:Color.LIMEGREEN);g.fillPolygon(new double[]{x,x+6,x,x-6},new double[]{y-6,y,y+6,y},4);}

    private void snapSelected(){var moving=selected();if(moving==null)return;ContactCandidate best=null;for(var other:circles)if(other.id()!=moving.id())for(int deg=0;deg<360;deg+=15){for(var type:Tangency.values()){double dist=type==Tangency.EXTERNAL?moving.radius()+other.radius():Math.abs(other.radius()-moving.radius());if(dist<1e-9)continue;double a=Math.toRadians(deg);double nx=other.x()+Math.sin(a)*dist,ny=other.y()-Math.cos(a)*dist;double d=Math.hypot(nx-moving.x(),ny-moving.y());if(d<30/zoom.get()&&(best==null||d<best.distance))best=new ContactCandidate(other,type,deg,nx,ny,d);}}if(best!=null){replace(moving.withPosition(best.x,best.y));int id=contacts.stream().mapToInt(ContactState::id).max().orElse(0)+1;double bDeg=normalize(best.degree+180);contacts.add(new ContactState(id,moving.id(),best.other.id(),best.type,bDeg,best.degree));discoverAllContacts(moving.id());}}
    private record ContactCandidate(CircleState other,Tangency type,double degree,double x,double y,double distance){}

    private void discoverAllContacts(int movedId){var m=find(movedId);if(m==null)return;for(var o:circles)if(o.id()!=movedId){if(contacts.stream().anyMatch(c->(c.aId()==movedId&&c.bId()==o.id())||(c.bId()==movedId&&c.aId()==o.id())))continue;double d=Math.hypot(o.x()-m.x(),o.y()-m.y());Tangency t=null;if(Math.abs(d-(m.radius()+o.radius()))<1e-5)t=Tangency.EXTERNAL;else if(Math.abs(d-Math.abs(m.radius()-o.radius()))<1e-5)t=Tangency.INTERNAL;if(t!=null){double od=normalize(Math.toDegrees(Math.atan2(m.x()-o.x(),-(m.y()-o.y()))));int id=contacts.stream().mapToInt(ContactState::id).max().orElse(0)+1;contacts.add(new ContactState(id,m.id(),o.id(),t,normalize(od+180),od));}}}
    private void repositionFromContact(int selected,int contactId){var ct=contacts.stream().filter(x->x.id()==contactId).findFirst().orElse(null);var c=find(selected);var o=ct==null?null:find(ct.other(selected));if(c==null||o==null)return;double d=ct.type()==Tangency.EXTERNAL?c.radius()+o.radius():Math.abs(o.radius()-c.radius());double a=Math.toRadians(ct.touchFor(o.id()));replace(c.withPosition(o.x()+Math.sin(a)*d,o.y()-Math.cos(a)*d));validateContacts();discoverAllContacts(c.id());}
    private void validateContacts(){contacts.removeIf(ct->{var a=find(ct.aId());var b=find(ct.bId());if(a==null||b==null)return true;double d=Math.hypot(a.x()-b.x(),a.y()-b.y());double exp=ct.type()==Tangency.EXTERNAL?a.radius()+b.radius():Math.abs(a.radius()-b.radius());return Math.abs(d-exp)>1e-4;});}

    private void addCircle(){int id=circles.stream().mapToInt(CircleState::id).max().orElse(0)+1;var c=new CircleState(id,100-panX/zoom.get()+id*12,100-panY/zoom.get()+id*12,60,0,false,0,null);circles.add(c);selectedId.set(id);}
    private void duplicateSelected(){var c=selected();if(c==null)return;int id=circles.stream().mapToInt(CircleState::id).max().orElse(0)+1;var n=new CircleState(id,c.x()+30,c.y()+30,c.radius(),c.startAngleDeg(),false,c.ownRateDegPerTick(),null);circles.add(n);selectedId.set(id);}
    private void deleteSelected(){int id=selectedId.get();if(id<0)return;contacts.removeIf(c->c.aId()==id||c.bId()==id);circles.removeIf(c->c.id()==id);selectedId.set(-1);}
    private void moveNoSnap(CircleState c,double dx,double dy){contacts.removeIf(ct->ct.aId()==c.id()||ct.bId()==c.id());replace(c.withPosition(c.x()+dx,c.y()+dy));}
    private void breakContact(int id){contacts.removeIf(c->c.id()==id);for(int i=0;i<circles.size();i++)if(Objects.equals(circles.get(i).slaveContactId(),id))circles.set(i,circles.get(i).withSlave(null));}
    private void fitAll(){if(circles.isEmpty())return;double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,maxY=-Double.MAX_VALUE;for(var c:circles){minX=Math.min(minX,c.x()-c.radius());minY=Math.min(minY,c.y()-c.radius());maxX=Math.max(maxX,c.x()+c.radius());maxY=Math.max(maxY,c.y()+c.radius());}double z=Math.min(canvas.getWidth()/(maxX-minX+100),canvas.getHeight()/(maxY-minY+100));zoom.set(Math.clamp(z,.1,8));panX=(canvas.getWidth()-(minX+maxX)*zoom.get())/2;panY=(canvas.getHeight()-(minY+maxY)*zoom.get())/2;redraw();}

    private void save(Stage s){var fc=new FileChooser();fc.setInitialFileName("workspace.json");var f=fc.showSaveDialog(s);if(f!=null)try{var d=data();store.save(f.toPath(),d);starting=d;}catch(Exception ex){alert(ex);}}
    private void load(Stage s){var fc=new FileChooser();var f=fc.showOpenDialog(s);if(f!=null)try{var d=store.load(f.toPath());starting=d;applyData(d);}catch(Exception ex){alert(ex);}}
    private WorkspaceData data(){return new WorkspaceData(List.copyOf(circles),List.copyOf(contacts),tick.get(),ticksPerSecond.get());}
    private void applyData(WorkspaceData d){running.set(false);circles.setAll(d.circles());contacts.setAll(d.contacts());tick.set(d.tick());ticksPerSecond.set(d.ticksPerSecond());selectedId.set(-1);redraw();}
    private void alert(Exception e){var a=new Alert(Alert.AlertType.ERROR,e.getMessage(),ButtonType.OK);a.showAndWait();}
    private CircleState selected(){return find(selectedId.get());}
    private CircleState find(int id){return circles.stream().filter(c->c.id()==id).findFirst().orElse(null);}
    private void replace(CircleState c){for(int i=0;i<circles.size();i++)if(circles.get(i).id()==c.id()){circles.set(i,c);return;}}
    private void replaceContact(ContactState c){for(int i=0;i<contacts.size();i++)if(contacts.get(i).id()==c.id()){contacts.set(i,c);return;}}
    private CircleState hit(double x,double y){return circles.stream().sorted(Comparator.comparingInt(CircleState::id).reversed()).filter(c->Math.hypot(x-c.x(),y-c.y())<=c.radius()).findFirst().orElse(null);}
    private double[] world(double x,double y){return new double[]{(x-panX)/zoom.get(),(y-panY)/zoom.get()};}
    private static double normalize(double d){d%=360;if(d<0)d+=360;return d;}
    private static String fmt(double v){return String.format(Locale.US,"%.6f",v);}
    private void addMetric(String name,double value,DoubleConsumer setter){addMetric(name,value,setter,false);} private void addMetric(String name,double value,DoubleConsumer setter,boolean disabled){addMetricTo(inspector,name,value,setter,disabled);}
    private void addMetricTo(VBox box,String name,double value,DoubleConsumer setter){addMetricTo(box,name,value,setter,false);} private void addMetricTo(VBox box,String name,double value,DoubleConsumer setter,boolean disabled){var f=doubleField(value,setter);f.setDisable(disabled);var row=new HBox(6,new Label(name),f);HBox.setHgrow(f,Priority.ALWAYS);box.getChildren().add(row);}
    private TextField doubleField(double v,DoubleConsumer setter){var f=new TextField(fmt(v));Runnable commit=()->{try{setter.accept(Double.parseDouble(f.getText().trim()));}catch(Exception e){f.setText(fmt(v));}};f.setOnAction(e->commit.run());f.focusedProperty().addListener((o,was,is)->{if(!is)commit.run();else javafx.application.Platform.runLater(f::selectAll);});return f;}
    private TextField longField(long v,LongConsumer setter){var f=new TextField(Long.toString(v));f.setOnAction(e->{try{setter.accept(Long.parseLong(f.getText().trim()));}catch(Exception ignored){}});f.focusedProperty().addListener((o,w,is)->{if(!is)try{setter.accept(Long.parseLong(f.getText().trim()));}catch(Exception ignored){}else javafx.application.Platform.runLater(f::selectAll);});return f;}
}
