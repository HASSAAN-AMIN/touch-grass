module com.touchgrass {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires java.sql;

    exports com.touchgrass;
    exports com.touchgrass.ui;
    exports com.touchgrass.bl;
    exports com.touchgrass.db;
    exports com.touchgrass.models;

    opens com.touchgrass to javafx.graphics;
    opens com.touchgrass.ui to javafx.graphics, javafx.fxml;
}
