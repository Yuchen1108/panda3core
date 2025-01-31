// PANDA 3 -- a domain-independent planner for classical and hierarchical planning
// Copyright (C) 2014-2018 the original author or authors.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package UUBenchmarksets.fromHTN.entertainment.problemGen;

import java.util.*;

/**
 * Created by dh on 20.05.17.
 */
public class heProblem {

    public List<heDevice> devices;
    public String name;
    private List<heDevice[]> aGoals;
    private List<heDevice[]> avGoals;
    private List<heDevice[]> vGoals;

    public heProblem(String name) {
        this.name = name;
        this.devices = new ArrayList<>();
        aGoals = new ArrayList<>();
        vGoals = new ArrayList<>();
        avGoals = new ArrayList<>();
    }

    public void addDevice(heDevice device) {
        this.devices.add(device);
    }

    @Override
    public String toString() {
        allNames = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        sb.append("(define (problem " + this.getName() + ")\n" +
                " (:domain entertainment)\n" +
                " (:objects\n");
        sb.append("  ");
        int devId = 1;
        for (heDevice dev : this.devices) {
            dev.setName(getNextName(dev.getType()));
            sb.append(dev.getName());
            sb.append(" ");
        }
        sb.append("- equipment\n");
        sb.append("  ");
        for (heDevice dev : this.devices) {
            for (String conn : dev.getConnections()) {
                sb.append(conn);
                sb.append(" ");
            }
        }
        sb.append("- connector\n");

        sb.append(" )\n" +
                " (:htn\n" +
                "  :ordered-tasks (and\n");

        for (heDevice[] goal : this.aGoals)
            sb.append("     (a_connect " + goal[0].getName() + " " + goal[1].getName() + ")\n");
        for (heDevice[] goal : this.vGoals)
            sb.append("     (v_connect " + goal[0].getName() + " " + goal[1].getName() + ")\n");
        for (heDevice[] goal : this.avGoals)
            sb.append("     (av_connect " + goal[0].getName() + " " + goal[1].getName() + ")\n");

        sb.append("   )\n" +
                "  :ordering ( )\n" +
                "  :constraints ( ))\n" +
                " (:init\n");

        for (heDevice dev : this.devices) {
            sb.append("  ;; device " + dev.getName() + "\n");
            List<String> conns = dev.getConnections();
            for (int i = 0; i < dev.connections.size(); i++) {
                String conn = conns.get(i);

                sb.append("  (conn_of " + dev.getName() + " " + conn + ")\n");
                sb.append("  (unused " + conn + ")\n");
                if (dev.isOutConn(i)) {
                    sb.append("  (out_connector " + conn + ")\n");
                }
                if (dev.isInConn(i)) {
                    sb.append("  (in_connector " + conn + ")\n");
                }
                if (dev.isAudioConn(i)) {
                    sb.append("  (audio_connector " + conn + ")\n");
                }
                if (dev.isVideoConn(i)) {
                    sb.append("  (video_connector " + conn + ")\n");
                }
            }
            sb.append("\n");
        }
        sb.append("  ;; compatibility of connections\n");
        for (int d1 = 0; d1 < this.devices.size(); d1++) {
            heDevice dev1 = this.devices.get(d1);
            List<String> conns1 = dev1.getConnections();
            for (int i = 0; i < dev1.connections.size(); i++) {
                for (int d2 = 0; d2 < this.devices.size(); d2++) {
                    heDevice dev2 = this.devices.get(d2);
                    List<String> conns2 = dev2.getConnections();
                    for (int j = 0; j < dev2.connections.size(); j++) {
                        if ((dev1.connections.get(i) == dev2.connections.get(j)) && (dev1.isPort(i) != dev2.isPort(j))) {
                            sb.append("  (compatible " + conns1.get(i) + " " + conns2.get(j) + ")\n");
                        }
                    }
                }
            }
        }
        sb.append(" )\n)\n");
        return sb.toString();
    }

    Set<String> allNames = new HashSet<>();

    private String getNextName(String type) {
        int i = 1;
        while (true) {
            if (allNames.contains(type + i)) {
                i++;
            } else {
                allNames.add(type + i);
                return type + i;
            }
        }
    }

    private void writeList(List<String> list, StringBuilder sb) {
        writeList(list, sb, "", "");
    }

    private void writeList(List<String> list, StringBuilder sb, String pref, String postf) {
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                sb.append("  ");
            } else {
                sb.append(" ");
            }
            sb.append(pref);
            sb.append(list.get(i));
            sb.append(postf);
        }
    }

    private int getNextIndex(List<String> cableDefs, String nameStr) {
        int i = 1;
        outerloop:
        while (true) {
            String lookFor = nameStr + i;
            for (String alreadyThere : cableDefs) {
                if (alreadyThere.equals(lookFor)) {
                    i++;
                    continue outerloop;
                }
            }
            return i;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAvGoal(heDevice[] g) {
        this.avGoals.add(g);
    }

    public void addAGoal(heDevice[] g) {
        this.aGoals.add(g);
    }

    public void addVGoal(heDevice[] g) {
        this.vGoals.add(g);
    }
}
