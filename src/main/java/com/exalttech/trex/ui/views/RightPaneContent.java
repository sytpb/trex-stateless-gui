package com.exalttech.trex.ui.views;

import com.exalttech.trex.application.TrexApp;
import com.exalttech.trex.core.RPCMethods;
import com.exalttech.trex.ui.controllers.MainViewController;
import com.exalttech.trex.ui.controllers.PortInfo.PortInfoTabConfig;
import com.exalttech.trex.ui.controllers.PortInfo.PortInfoTabMain;
import com.exalttech.trex.ui.models.Port;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Created by ichebyki on 07.02.17.
 */
public class RightPaneContent {

    private static final Logger LOG = Logger.getLogger(MainViewController.class.getName());

    HashMap<Port, TabPane> portRightPaneContentMap;

    public RightPaneContent() {
        portRightPaneContentMap = new HashMap<>(2);
    }


    public Node generatePortInfoPane(RPCMethods serverRPCMethods, Port port) {
        Tab tab;
        TabPane tabPanePortInfo = portRightPaneContentMap.get(port);

        if (tabPanePortInfo == null) {
            tabPanePortInfo = new TabPane();
            portRightPaneContentMap.put(port, tabPanePortInfo);
            tabPanePortInfo.getStyleClass().addAll("statRightPaneContent", "floating");
        }

        if (tabPanePortInfo.getTabs().size() == 0) {
            PortInfoTabMain   rootPortInfoTabMain;
            PortInfoTabConfig rootPortInfoTabConfig;

            tab = new Tab("Main");
            tab.setClosable(false);
            tabPanePortInfo.getTabs().add(0, tab);
            tab = new Tab("Layer Configuration");
            tab.setClosable(false);
            tabPanePortInfo.getTabs().add(1, tab);

            try {
                tab = tabPanePortInfo.getTabs().get(0);
                rootPortInfoTabMain = new PortInfoTabMain(TrexApp.injector, serverRPCMethods, port);
                tab.setContent(rootPortInfoTabMain);
            } catch (Exception e) {
                LOG.error("Failed to create port info right pane: ", e);
            }

            try {
                tab = tabPanePortInfo.getTabs().get(1);
                rootPortInfoTabConfig = new PortInfoTabConfig(TrexApp.injector, serverRPCMethods, port);
                tab.setContent(rootPortInfoTabConfig);
            } catch (Exception e) {
                LOG.error("Failed to create port info right pane: ", e);
            }
        }
        else {
            ((PortInfoTabMain) tabPanePortInfo.getTabs().get(0).getContent()).update(false);
            ((PortInfoTabConfig) tabPanePortInfo.getTabs().get(1).getContent()).update(false);
        }

        return tabPanePortInfo;
    }

}