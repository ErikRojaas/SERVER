module com.erikxavi.barretina {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.erikxavi.barretina to javafx.fxml;
    exports com.erikxavi.barretina;
}