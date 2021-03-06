/**
 * *****************************************************************************
 * Copyright (c) 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************
 */
package com.exalttech.trex.ui.controllers;

import com.exalttech.trex.remote.models.profiles.Mode;
import com.exalttech.trex.remote.models.profiles.Profile;
import com.exalttech.trex.remote.models.profiles.Rate;
import com.exalttech.trex.ui.components.NumberField;
import com.exalttech.trex.util.Util;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;


public class StreamPropertiesViewController implements Initializable, EventHandler<KeyEvent> {
    // Mode
    @FXML
    private ToggleGroup streamModeGroup;
    @FXML
    private RadioButton continuousMode;
    @FXML
    private RadioButton burstMode;
    @FXML
    private RadioButton multiBurstMode;
    // Misc
    @FXML
    private CheckBox enabledCB;
    @FXML
    private CheckBox selfStartCB;
    // Numbers
    @FXML
    private VBox numbersContainer;
    @FXML
    private TextField numOfPacketTB;
    @FXML
    private TextField numOfBurstTB;
    @FXML
    private TextField packetPBurstTB;
    @FXML
    private Label numOfPacketLabel;
    @FXML
    private Label packetPBurstTitle;
    @FXML
    private Label numOfBurstLabel;
    // Rate
    @FXML
    private ComboBox<String> rateTypeCB;
    @FXML
    private NumberField rateValueTF;
    // Next Stream
    @FXML
    private VBox afterStreamContainer;
    @FXML
    private ToggleGroup nextStreamGroup;
    @FXML
    private RadioButton stopRG;
    @FXML
    private RadioButton gotoRG;
    @FXML
    private ComboBox nextStreamCB;
    @FXML
    private CheckBox timeInLoopCB;
    @FXML
    private TextField timeInLoopTF;
    // Gaps
    @FXML
    private ImageView gapsImageContainer;
    @FXML
    private TextField isgTF;
    @FXML
    private TextField ibgTF;
    @FXML
    private Label ibgTitle;
    @FXML
    private Label ipdL;
    @FXML
    private TextField ipgTF;
    // Rx Stats
    @FXML
    private CheckBox rxEnableCB;
    @FXML
    private CheckBox rxLatencyCB;
    @FXML
    private TextField rxStreamID;
    @FXML
    private Label rxStreamIDLabel;

    private List<Profile> profileList;
    private Profile selectedProfile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initStreamPropertiesEvent();
    }

    public void init(List<Profile> profileList, int selectedProfileIndex) {
        this.profileList = profileList;
        this.selectedProfile = profileList.get(selectedProfileIndex);
        fillStreamProperties(selectedProfileIndex);
    }

    private void initStreamPropertiesEvent() {
        timeInLoopTF.disableProperty().bind(timeInLoopCB.selectedProperty().not());
        nextStreamCB.disableProperty().bind(gotoRG.selectedProperty().not());

        streamModeGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            if (newValue == continuousMode) {
                handleContinousModeSelection();
            } else if (newValue == burstMode) {
                handleBurstModeSelection();
            } else {
                handleMultiBurstModeSelection();
            }
        });

        nextStreamGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            boolean disableTimeToLoopCB = true;
            if (newValue == gotoRG) {
                disableTimeToLoopCB = false;
            }
            timeInLoopCB.setSelected(false);
            timeInLoopCB.setDisable(disableTimeToLoopCB);
        });

        timeInLoopCB.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (!newValue) {
                timeInLoopTF.setText("0");
            }
        });

        rateTypeCB.getSelectionModel().selectedItemProperty().addListener(this::handleRateTypeChanged);
        rateValueTF.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double ipgValue = 0;
            if (newValue != null && newValue.doubleValue() > 0) {
                ipgValue = 1.0 / newValue.doubleValue();
            }
            ipgTF.setText(Util.getFormatedFraction(ipgValue));
        });

        // bind RX fields with rx enable property
        rxStreamID.disableProperty().bind(rxEnableCB.selectedProperty().not());
        rxStreamIDLabel.disableProperty().bind(rxEnableCB.selectedProperty().not());
        rxLatencyCB.disableProperty().bind(rxEnableCB.selectedProperty().not());

        // add key press event to allow digits only
        numOfPacketTB.addEventFilter(KeyEvent.KEY_TYPED, this);
        numOfBurstTB.addEventFilter(KeyEvent.KEY_TYPED, this);
        packetPBurstTB.addEventFilter(KeyEvent.KEY_TYPED, this);
        timeInLoopTF.addEventFilter(KeyEvent.KEY_TYPED, this);
        rxStreamID.addEventFilter(KeyEvent.KEY_TYPED, this);

        // set input validation on ibg field
        final UnaryOperator<TextFormatter.Change> formatter = Util.getTextChangeFormatter(Util.getUnitRegex(true));
        ibgTF.setTextFormatter(new TextFormatter<>(formatter));
        isgTF.setTextFormatter(new TextFormatter<>(formatter));
        
        // allow only 5 digits
        timeInLoopTF.setTextFormatter(Util.getNumberFilter(5));
    }

    private String getSelectedRateType() {
        return rateTypeCB.getValue().replace(' ', '_');
    }

    private void handleRateTypeChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        final Rate rate = selectedProfile.getStream().getMode().getRate();
        final String rateType = rate.getType();
        final String selectedRateType = getSelectedRateType();
        if (selectedRateType.equals(Rate.RateTypes.PERCENTAGE)) {
            rateValueTF.setMaxValue(100.0);
            rateValueTF.setAllowUnits(false);
        } else {
            rateValueTF.setMaxValue(null);
            rateValueTF.setAllowUnits(true);
        }
        if (selectedRateType.equals(Rate.RateTypes.PPS)) {
            ipdL.setVisible(true);
            ipgTF.setVisible(true);
        } else {
            ipdL.setVisible(false);
            ipgTF.setVisible(false);
        }
        if (selectedRateType.equals(rateType)) {
            rateValueTF.setValue(rate.getValue());
        } else {
            rateValueTF.setValue(1.0);
        }
    }

    private void handleContinousModeSelection() {
        streamModeGroup.selectToggle(continuousMode);
        streamModeGroup.setUserData(StreamMode.CONTINUOUS);

        // disable numbers 
        numbersContainer.setDisable(true);
        packetPBurstTitle.setDisable(false);
        packetPBurstTB.setDisable(false);
        numOfBurstTB.setText("1");

        // disable next stream
        afterStreamContainer.setDisable(true);
        stopRG.setSelected(true);

        // disable ibg
        ibgTF.setDisable(true);
        ibgTitle.setDisable(true);

        // define gaps view
        gapsImageContainer.setImage(new Image("/icons/" + StreamMode.CONTINUOUS.getImageName()));
    }

    private void handleBurstModeSelection() {
        streamModeGroup.selectToggle(burstMode);
        streamModeGroup.setUserData(StreamMode.SINGLE_BURST);

        // disable/hide part of numbers 
        numbersContainer.setDisable(false);
        numOfPacketLabel.setDisable(false);
        numOfPacketTB.setDisable(false);
        numOfBurstLabel.setDisable(true);
        numOfBurstTB.setDisable(true);
        numOfBurstTB.setText("1");
        packetPBurstTitle.setDisable(true);
        packetPBurstTB.setDisable(true);

        // enable next stream
        afterStreamContainer.setDisable(false);

        // disable ibg
        ibgTF.setDisable(true);
        ibgTitle.setDisable(true);

        // define gaps view
        gapsImageContainer.setImage(new Image("/icons/" + StreamMode.SINGLE_BURST.getImageName()));
    }

    private void handleMultiBurstModeSelection() {
        streamModeGroup.selectToggle(multiBurstMode);
        streamModeGroup.setUserData(StreamMode.MULTI_BURST);

        // disable numbers
        numbersContainer.setDisable(false);
        numOfPacketLabel.setDisable(true);
        numOfPacketTB.setDisable(true);
        numOfBurstLabel.setDisable(false);
        numOfBurstTB.setDisable(false);
        packetPBurstTitle.setDisable(false);
        packetPBurstTB.setDisable(false);

        // enable next stream
        afterStreamContainer.setDisable(false);

        // enable ibg
        ibgTF.setDisable(false);
        ibgTitle.setDisable(false);

        // define gaps view
        gapsImageContainer.setImage(new Image("/icons/" + StreamMode.MULTI_BURST.getImageName()));

        // set numOf Burst value
        Mode mode = selectedProfile.getStream().getMode();
        int numOfBurst = mode.getCount();
        if (numOfBurst < 2) {
            numOfBurst = 2;
        }
        numOfBurstTB.setText(String.valueOf(numOfBurst));
    }

    @Override
    public void handle(KeyEvent event) {
        if (!event.getCharacter().matches("[0-9]") && event.getCode() != KeyCode.BACK_SPACE) {
            event.consume();
        }
    }

    private void fillStreamProperties(int currentSelectedIndex) {
        Mode mode = selectedProfile.getStream().getMode();
        enabledCB.setSelected(selectedProfile.getStream().isEnabled());
        selfStartCB.setSelected(selectedProfile.getStream().isSelfStart());
        numOfPacketTB.setText(String.valueOf(mode.getTotalPkts()));
        packetPBurstTB.setText(String.valueOf(mode.getPacketsPerBurst()));
        numOfBurstTB.setText(String.valueOf(mode.getCount()));
        rateTypeCB.getSelectionModel().select(mode.getRate().getType().replace('_', ' '));
        rateValueTF.setValue(mode.getRate().getValue());
        isgTF.setText(convertNumToUnit(selectedProfile.getStream().getIsg()));
        ibgTF.setText(convertNumToUnit(mode.getIbg()));

        rxStreamID.setText(String.valueOf(selectedProfile.getStream().getFlowStats().getStreamID()));
        rxEnableCB.setSelected(selectedProfile.getStream().getFlowStats().getEnabled());
        rxLatencyCB.setSelected(selectedProfile.getStream().getFlowStats().isLatencyEnabled());

        fillGotoStreamOption(currentSelectedIndex);
        stopRG.setSelected(true);
        if (!"-1".equals(selectedProfile.getNext())) {
            gotoRG.setSelected(true);
        }
        timeInLoopTF.setText(String.valueOf(selectedProfile.getStream().getActionCount()));
        timeInLoopCB.setSelected(selectedProfile.getStream().getActionCount() > 0);

        StreamMode streamMode = StreamMode.CONTINUOUS;
        if (!Util.isNullOrEmpty(mode.getType())) {
            streamMode = StreamMode.getMode(mode.getType());
        }
        switch (streamMode) {
            case CONTINUOUS:
                handleContinousModeSelection();
                break;
            case SINGLE_BURST:
                handleBurstModeSelection();
                break;
            case MULTI_BURST:
                handleMultiBurstModeSelection();
                break;
            default:
                break;
        }
    }

    private void fillGotoStreamOption(int currentSelectedIndex) {
        nextStreamCB.getItems().clear();
        if (currentSelectedIndex > 0) {
            nextStreamCB.getItems().add("First Stream");
        }
        for (Profile p : profileList) {
            if (!p.getName().equalsIgnoreCase(selectedProfile.getName())) {
                nextStreamCB.getItems().add(p.getName());
            }
            if (p.getName().equalsIgnoreCase(selectedProfile.getNext())) {
                nextStreamCB.getSelectionModel().select(p.getName());
            }
        }
    }

    Profile getUpdatedSelectedProfile() throws Exception {
        // update Misc
        selectedProfile.getStream().setEnabled(enabledCB.isSelected());
        selectedProfile.getStream().setSelfStart(selfStartCB.isSelected());

        String ruleType = null;
        // update rx
        selectedProfile.getStream().getFlowStats().setEnabled(rxEnableCB.isSelected());
        if (rxEnableCB.isSelected()) {
            selectedProfile.getStream().getFlowStats().setStreamID(Util.getIntFromString(rxStreamID.getText()));
            
            ruleType = rxLatencyCB.isSelected() ? "latency": "stats";
            selectedProfile.getStream().getFlowStats().setRuleType(ruleType);
        }


        switch ((StreamMode) streamModeGroup.getUserData()) {
            case CONTINUOUS:
                updateContinuousProfile(selectedProfile);
                break;
            case SINGLE_BURST:
                updateSingleBurstProfile(selectedProfile);
                break;
            case MULTI_BURST:
                updateMultiBurstProfile(selectedProfile);
                break;
            default:
                break;
        }
        return selectedProfile;
    }

    private void updateContinuousProfile(Profile profile) {
        // update mode
        profile.getStream().getMode().setType(StreamMode.CONTINUOUS.toString());

        // update rate
        profile.getStream().getMode().getRate().setType(getSelectedRateType());
        profile.getStream().getMode().getRate().setValue(rateValueTF.getValue());

        // update next stream 
        updateNextStream(profile);
        // gaps
        profile.getStream().setIsg(convertUnitToNum(isgTF.getText()));
    }

    private void updateSingleBurstProfile(Profile profile) {
        // update mode
        profile.getStream().getMode().setType(StreamMode.SINGLE_BURST.toString());

        // update numbers
        profile.getStream().getMode().setTotalPkts(getIntValue(numOfPacketTB.getText()));

        //no property for number of burst yet to update
        profile.getStream().getMode().setPacketsPerBurst(0);

        // update rate
        profile.getStream().getMode().getRate().setType(getSelectedRateType());
        profile.getStream().getMode().getRate().setValue(rateValueTF.getValue());

        // update next stream
        updateNextStream(profile);

        // gaps
        profile.getStream().setIsg(convertUnitToNum(isgTF.getText()));
    }

    private void updateMultiBurstProfile(Profile profile) {
        // update mode
        profile.getStream().getMode().setType(StreamMode.MULTI_BURST.toString());

        // update numbers
        profile.getStream().getMode().setPacketsPerBurst(getIntValue(packetPBurstTB.getText()));

        // update number of bursts
        profile.getStream().getMode().setCount(getIntValue(numOfBurstTB.getText()));

        // update rate
        profile.getStream().getMode().getRate().setType(getSelectedRateType());
        profile.getStream().getMode().getRate().setValue(rateValueTF.getValue());

        // update next stream
        updateNextStream(profile);

        // gaps
        profile.getStream().setIsg(convertUnitToNum(isgTF.getText()));
        String ibgValue = !Util.isNullOrEmpty(ibgTF.getText()) ? ibgTF.getText() : "0.0";
        profile.getStream().getMode().setIbg(convertUnitToNum(ibgValue));
    }

    boolean isValidStreamPropertiesFields() {
        String errMsg = "";
        boolean valid = true;
        if (selectedProfile.getStream().getPacket().getBinary() == null) {
            errMsg = "Please load a Pcap file";
            valid = false;
        } else {
            double timeInloop = Double.parseDouble(timeInLoopTF.getText());
            if (timeInLoopCB.isSelected() && (timeInloop <= 0 || timeInloop > 64000)) {
                errMsg = "Time in loop should be between > 0 and < 64K";
                valid = false;
            }
        }
        boolean validInputData = validInputData();
        if (!valid && validInputData) {
            Alert alert = Util.getAlert(Alert.AlertType.ERROR);
            alert.setContentText(errMsg);
            alert.showAndWait();
        }
        return valid && validInputData;
    }

    private boolean validInputData() {
        String errMsg = "";
        boolean valid = true;
        if (rateValueTF.getValue() <= 0) {
            errMsg = "Rate value should be > 0";
            valid = false;
        } else if ((Util.isNullOrEmpty(numOfPacketTB.getText()) || Double.parseDouble(numOfPacketTB.getText()) <= 0)
                && (StreamMode) streamModeGroup.getUserData() == StreamMode.SINGLE_BURST) {
            errMsg = "Number of packets should be > 0";
            valid = false;
        } else if ((StreamMode) streamModeGroup.getUserData() == StreamMode.MULTI_BURST) {
            if (Util.isNullOrEmpty(numOfBurstTB.getText()) || Double.parseDouble(numOfBurstTB.getText()) < 2) {
                errMsg = "Number of burst should be > 1";
                valid = false;
            } else if (Util.isNullOrEmpty(packetPBurstTB.getText()) || Double.parseDouble(packetPBurstTB.getText()) <= 0) {
                errMsg = "Packer per Burst should be > 0";
                valid = false;
            }
        }
        if (!valid) {
            Alert alert = Util.getAlert(Alert.AlertType.ERROR);
            alert.setContentText(errMsg);
            alert.showAndWait();
        }
        return valid;
    }

    private void updateNextStream(Profile profile) {
        profile.setNext("-1");
        if (nextStreamGroup.getSelectedToggle() == gotoRG) {
            profile.setNext(String.valueOf(nextStreamCB.getValue()));
            if ("First Stream".equals(nextStreamCB.getValue())) {
                profile.setNext(profileList.get(0).getName());
            }

            profile.getStream().setActionCount(Integer.parseInt(timeInLoopTF.getText()));
        }
    }

    private int getIntValue(String text) {
        return Util.isNullOrEmpty(text) ? 0 : Integer.parseInt(text);
    }

    private double convertUnitToNum(String valueData) {
        return Util.convertUnitToNum(valueData) * 1000000;
    }

    private String convertNumToUnit(double value) {
        return Util.convertNumToUnit(value / 1000000);
    }

    private enum StreamMode {
        CONTINUOUS,
        SINGLE_BURST,
        MULTI_BURST;

        public String getImageName() {
            return name().toLowerCase() + ".png";
        }

        public static StreamMode getMode(String modeName) {
            return StreamMode.valueOf(modeName.toUpperCase());
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
