package officerextension;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class EconomyListener extends BaseCampaignEventListener  {

    public EconomyListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        super.reportEconomyTick(iterIndex);
        if (TutorialMissionIntel.isTutorialInProgress()) {
            return;
        }

        // Add suspended officers' salary to the monthly report
        float f = 1f / Global.getSettings().getFloat("economyIterPerMonth");
        MonthlyReport report = SharedData.getData().getCurrentReport();
        FDNode fleetNode = report.getNode(MonthlyReport.FLEET);
        FDNode officerNode = report.getNode(fleetNode, MonthlyReport.OFFICERS);
        FDNode suspendedNode = report.getNode(officerNode, Settings.SUSPENDED_OFFICERS_NODE);
        suspendedNode.name = "Suspended officers";
        suspendedNode.custom = Settings.SUSPENDED_OFFICERS_NODE;
        suspendedNode.tooltipCreator = report.getMonthlyReportTooltip();
        for (OfficerDataAPI officer : Util.getSuspendedOfficers()) {
            float salary = Settings.SUSPENDED_SALARY_FRACTION * Misc.getOfficerSalary(officer.getPerson());
            FDNode child = report.getNode(suspendedNode, officer.getPerson().getId());
            child.name = officer.getPerson().getNameString();
            child.upkeep += f * salary;
            child.custom = officer;
            // Undo the suspended officers' salary in the base officer node
            FDNode originalChild = report.getNode(officerNode, officer.getPerson().getId());
            originalChild.upkeep -= f * Misc.getOfficerSalary(officer.getPerson());
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        MonthlyReport report = SharedData.getData().getPreviousReport();
        FDNode officerNode = report.getNode(MonthlyReport.FLEET, MonthlyReport.OFFICERS);

        // It seems like the game is already doing some sorting of its own (i.e. stipend seems to be
        // hard-coded to be shown first in the Fleet tab), so we'll just sort only the "Officer payroll" node
        sortFDNode(officerNode, new Comparator<Map.Entry<String, FDNode>>() {
            @Override
            public int compare(Map.Entry<String, FDNode> node1, Map.Entry<String, FDNode> node2) {
                // Always display non-leaf children last
                int size1 = node1.getValue().getChildren().size();
                int size2 = node2.getValue().getChildren().size();
                if (size1 > 0 && size2 == 0) {
                    return 1;
                }
                if (size1 == 0 && size2 > 0) {
                    return -1;
                }
                // Then, sort by descending order of upkeep - income (adjusted upkeep)
                float upkeep1 = node1.getValue().upkeep - node1.getValue().income;
                float upkeep2 = node2.getValue().upkeep - node2.getValue().income;
                if (upkeep1 != upkeep2) {
                    return Float.compare(upkeep2, upkeep1);
                }
                // If they have the same upkeep, sort alphabetically
                return node1.getValue().name.compareTo(node2.getValue().name);
            }
        });
    }

    private void sortFDNode(FDNode node, Comparator<Map.Entry<String, FDNode>> ctr) {
        for (FDNode child : node.getChildren().values()) {
            sortFDNode(child, ctr);
        }

        List<Map.Entry<String, FDNode>> entries = new ArrayList<>(node.getChildren().entrySet());
        Collections.sort(entries, ctr);

        node.getChildren().clear();
        for (Map.Entry<String, FDNode> entry : entries) {
            node.getChildren().put(entry.getKey(), entry.getValue());
        }
    }
}
