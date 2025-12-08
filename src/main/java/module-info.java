module pkg.vms {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.base;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens pkg.vms to javafx.fxml;
    exports pkg.vms;
    exports pkg.vms.model;
    exports pkg.vms.controller to javafx.fxml;
    exports pkg.vms.controller.layout;
    opens pkg.vms.controller to javafx.fxml;
    opens pkg.vms.model to javafx.fxml;
    opens pkg.vms.controller.layout to javafx.fxml;
}