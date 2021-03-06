package com.cisco.trex.stl.gui.controllers.dashboard.global;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.stage.WindowEvent;

import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cisco.trex.stl.gui.controllers.dashboard.GlobalStatsBaseController;
import com.cisco.trex.stl.gui.storages.PGIDsStorage;
import com.cisco.trex.stl.gui.storages.StatsStorage;

import com.exalttech.trex.ui.PortsManager;
import com.exalttech.trex.ui.views.statistics.StatsLoader;
import com.exalttech.trex.util.Initialization;
import com.exalttech.trex.util.Util;


public class GlobalStatsController extends GlobalStatsBaseController {
    private static final Logger LOG = Logger.getLogger(GlobalStatsController.class.getName());

    @FXML
    private AnchorPane root;
    @FXML
    private GlobalStatsPanelController cpu;
    @FXML
    private GlobalStatsPanelController rxCpu;
    @FXML
    private GlobalStatsPanelController totalTx;
    @FXML
    private GlobalStatsPanelController totalTxL1;
    @FXML
    private GlobalStatsPanelController totalRx;
    @FXML
    private GlobalStatsPanelController totalPps;
    @FXML
    private GlobalStatsPanelController totalStream;
    @FXML
    private GlobalStatsPanelController activePort;
    @FXML
    private GlobalStatsPanelController dropRate;
    @FXML
    private GlobalStatsPanelController queueFull;

    public GlobalStatsController() {
        Initialization.initializeFXML(this, "/fxml/dashboard/global/GlobalStats.fxml");
        Initialization.initializeCloseEvent(root, this::onWindowCloseRequest);
    }

    @Override
    protected void render() {
        Map<String, String> currentStatsList = StatsLoader.getInstance().getLoadedStatsList();

        String cpuData = currentStatsList.get("m_cpu_util");
        if (Util.isNullOrEmpty(cpuData)) {
            cpuData = "0";
        }
        cpu.setValue(String.format(Locale.US, "%.2f %%", Double.parseDouble(cpuData)));

        String rxCpuData = currentStatsList.get("m_rx_cpu_util");
        if (Util.isNullOrEmpty(rxCpuData)) {
            rxCpuData = "0";
        }
        rxCpu.setValue(String.format(Locale.US, "%.2f %%", Double.parseDouble(rxCpuData)));

        double m_tx_bps = Double.parseDouble(currentStatsList.get("m_tx_bps"));
        double m_tx_pps = Double.parseDouble(currentStatsList.get("m_tx_pps"));
        // L1 Tx == "m_tx_bps" + 20 * "m_tx_pps" * 8.0
        double l1_tx_bps = m_tx_bps + m_tx_pps * 20.0 * 8.0;
        String queue = getQueue(currentStatsList);

        totalTx.setValue(Util.getFormatted(String.valueOf(m_tx_bps), true, "b/s"));
        totalTxL1.setValue(Util.getFormatted(String.valueOf(l1_tx_bps), true, "b/s"));
        totalRx.setValue(Util.getFormatted(currentStatsList.get("m_rx_bps"), true, "b/s"));
        totalPps.setValue(Util.getFormatted(String.valueOf(m_tx_pps), true, "pkt/s"));
        activePort.setValue(PortsManager.getInstance().getActivePort());
        dropRate.setValue(Util.getFormatted(currentStatsList.get("m_rx_drop_bps"), true, "b/s"));
        queueFull.setValue(Util.getFormatted(queue, true, "pkts"));

        final PGIDsStorage pgIdStatsStorage = StatsStorage.getInstance().getPGIDsStorage();
        synchronized (pgIdStatsStorage.getDataLock()) {
            totalStream.setValue(String.valueOf(pgIdStatsStorage.getPgIDs().size()));
        }
    }

    private void onWindowCloseRequest(WindowEvent window) {
        setActive(false);
    }

    private static String getQueue(Map<String, String> currentStatsList) {
        try {
            String current = currentStatsList.get("m_total_queue_full");
            return String.valueOf(Double.parseDouble(current));
        } catch (NumberFormatException e) {
            LOG.error("Error calculating queue full value", e);
            return "0";
        }
    }

    static double round(double value) {
        return ((int)(value*100))/100.0;
    }
}
